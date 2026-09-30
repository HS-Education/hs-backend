package com.hs.hstesis.repo.infrastructure.storage.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.FileStorageService;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.exceptions.FileStorageUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "azure-blob")
public class FileStorageBlobAdapter implements FileStorageService {
    private final BlobServiceClient storage;
    private final String container;
    public FileStorageBlobAdapter(BlobServiceClient storage,
                                  @Value("${azure.storage.documents-container}") String container) {
        this.storage = storage;
        this.container = container;
    }

    @Override
    public String generateObjectKey(String originalFileName, Long topicId) {
        return "documents/" + topicId + "/" + UUID.randomUUID() + ".pdf";
    }

    @Override
    public String calculateChecksum(UploadFile file) {
        try (InputStream input = file.openStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) { throw new FileStorageUnavailableException("calculate checksum", e); }
    }

    @Override
    public void upload(UploadFile file, String objectKey) {
        requireDocumentKey(objectKey);
        try (InputStream input = file.openStream()) {
            var blob = storage.getBlobContainerClient(container).getBlobClient(objectKey);
            blob.upload(input, file.size(), false);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType("application/pdf"));
        } catch (Exception e) { throw new FileStorageUnavailableException("upload", e); }
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        requireDocumentKey(objectKey);
        try {
            var now = OffsetDateTime.now();
            var blob = storage.getBlobContainerClient(container).getBlobClient(objectKey);
            var key = storage.getUserDelegationKey(now.minusMinutes(1), now.plusMinutes(15));
            var values = new BlobServiceSasSignatureValues(now.plusMinutes(10),
                    new BlobSasPermission().setReadPermission(true)).setStartTime(now.minusMinutes(1))
                    .setProtocol(SasProtocol.HTTPS_ONLY);
            return blob.getBlobUrl() + "?" + blob.generateUserDelegationSas(values, key);
        } catch (Exception e) { throw new FileStorageUnavailableException("generate download url", e); }
    }

    @Override
    public void delete(String objectKey) {
        requireDocumentKey(objectKey);
        try { storage.getBlobContainerClient(container).getBlobClient(objectKey).deleteIfExists(); }
        catch (Exception e) { throw new FileStorageUnavailableException("delete object", e); }
    }

    static void requireDocumentKey(String key) {
        if (key == null || !key.startsWith("documents/") || key.contains("..") || key.contains("\\")
                || key.contains("?") || key.contains("#") || key.length() > 1024) {
            throw new IllegalArgumentException("Invalid document object key");
        }
    }
}
