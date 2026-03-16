package com.hs.hstesis.repo.domain.model.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class FileStorageInfo {
    private String originalFileName;
    private String objectKey;
    private String fileChecksum;

    public FileStorageInfo() {
    }

    public FileStorageInfo(String originalFileName, String objectKey, String fileChecksum) {
        this.originalFileName = originalFileName;
        this.objectKey = objectKey;
        this.fileChecksum = fileChecksum;
    }
}
