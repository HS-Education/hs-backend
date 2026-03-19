package com.hs.hstesis.repo.interfaces.rest.resources;

import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;

import java.util.List;

public record UploadDocumentResource(String title,
                                     Long topicId,
                                     EducationLevel educationLevel,
                                     List<GradeLevel> gradeLevels) {}