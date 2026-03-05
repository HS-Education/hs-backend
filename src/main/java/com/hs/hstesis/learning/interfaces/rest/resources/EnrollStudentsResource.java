package com.hs.hstesis.learning.interfaces.rest.resources;

import java.util.List;

public record EnrollStudentsResource(List<Long> studentIds, Long academicLevelId, Long academicYearId) { }
