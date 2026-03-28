package com.hs.hstesis.repo.application.internal.outboundservices.storage;

public interface FileStorageService {
    String generateObjectKey(String originalFileName, Long topicId);
    String calculateChecksum(UploadFile file);
    void upload(UploadFile file, String objectKey);
    String generatePresignedUrl(String objectKey);
    void delete(String objectKey);
}
