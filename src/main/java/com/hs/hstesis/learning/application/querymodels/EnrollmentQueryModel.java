package com.hs.hstesis.learning.application.querymodels;

public record EnrollmentQueryModel(Long id,
                                   Long userId,
                                   String userName,
                                   String roleInClassroom) {}
