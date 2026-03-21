package com.hs.hstesis.repo.domain.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String generateObjectKey(String originalFileName, Long topicId);
    String calculateChecksum(MultipartFile file);
    void upload(MultipartFile file, String objectKey);
    String generatePresignedUrl(String objectKey);
    void delete(String objectKey);
}
