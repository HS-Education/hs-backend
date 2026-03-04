package com.hs.hstesis.learning.domain.exceptions;

public class AcademicLevelRelatedToSectionsException extends RuntimeException {
    public AcademicLevelRelatedToSectionsException(Long id) {
        super(String.format("Academic level with id %d cannot be deleted because it is related to sections.", id));
    }
}
