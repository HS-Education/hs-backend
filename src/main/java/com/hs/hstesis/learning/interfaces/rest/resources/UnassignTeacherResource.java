package com.hs.hstesis.learning.interfaces.rest.resources;

import java.util.List;

public record UnassignTeacherResource(Long teacherId, List<Long> classroomIds) { }
