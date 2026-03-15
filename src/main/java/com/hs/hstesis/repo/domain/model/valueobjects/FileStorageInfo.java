package com.hs.hstesis.repo.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public class FileStorageInfo {
    private String originalFileName;
    private String objectKey;
    private String fileChecksum;
}
