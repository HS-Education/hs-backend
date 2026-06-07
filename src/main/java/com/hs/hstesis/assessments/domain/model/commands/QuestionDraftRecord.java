package com.hs.hstesis.assessments.domain.model.commands;

import java.util.List;

public record QuestionDraftRecord(
        String text,
        List<String> options,
        Integer correctOptionIndex
) {
}
