package com.hs.hstesis.learning.domain.exceptions;

import com.hs.hstesis.learning.domain.model.valueobjects.Bimester;

public class InvalidGradingPeriodDeleteException extends RuntimeException {
    public InvalidGradingPeriodDeleteException(Bimester bimester, String yearName) {
        super(String.format("Cannot delete bimester '%s' for academic year '%s' because it has already started or ended.",
                bimester, yearName));
    }
}
