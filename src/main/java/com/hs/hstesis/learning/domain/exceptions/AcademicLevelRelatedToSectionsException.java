package com.hs.hstesis.learning.domain.exceptions;

public class AcademicLevelRelatedToSectionsException extends RuntimeException {
    public AcademicLevelRelatedToSectionsException(String name) {
        super(String.format("Academic level '%s' cannot be deleted because it has associated sections.", name));
    }
}
