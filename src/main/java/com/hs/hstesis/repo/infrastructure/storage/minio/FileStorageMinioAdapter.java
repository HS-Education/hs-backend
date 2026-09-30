package com.hs.hstesis.repo.infrastructure.storage.minio;

import com.hs.hstesis.repo.application.internal.outboundservices.storage.UploadFile;
import com.hs.hstesis.repo.domain.exceptions.FileStorageUnavailableException;
import com.hs.hstesis.repo.application.internal.outboundservices.storage.FileStorageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
public class FileStorageMinioAdapter implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public FileStorageMinioAdapter(
            MinioClient minioClient,
            @Value("${minio.bucket}") String bucketName
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void initBucket() {

        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }

        } catch (Exception e) {
            throw new FileStorageUnavailableException("init bucket", e);
        }
    }

    @Override
    public String generateObjectKey(String originalFileName, Long topicId) {
        String safeName = originalFileName.replaceAll("\\s+", "_");
        return "documents/" + topicId + "/" + UUID.randomUUID() + "_" + safeName;
    }

    @Override
    public String calculateChecksum(UploadFile file) {

        try (InputStream is = file.openStream()) {

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(is.readAllBytes());

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new FileStorageUnavailableException("calculate checksum", e);
        }
    }

    @Override
    public void upload(UploadFile file, String objectKey) {

        try (InputStream is = file.openStream()) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(is, file.size(), -1)
                            .contentType(file.contentType())
                            .build()
            );

        } catch (Exception e) {
            throw new FileStorageUnavailableException("upload", e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey) {

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(60 * 10)
                            .build()
            );
        } catch (Exception e) {
            throw new FileStorageUnavailableException("generate presigned url", e);
        }
    }

    @Override
    public void delete(String objectKey) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) {
                return;
            }
            throw new FileStorageUnavailableException("delete object", e);
        } catch (Exception e) {
            throw new FileStorageUnavailableException("delete object", e);
        }
    }

}
