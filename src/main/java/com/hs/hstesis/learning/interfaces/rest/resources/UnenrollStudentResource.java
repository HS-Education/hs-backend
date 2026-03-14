package com.hs.hstesis.learning.interfaces.rest.resources;

import java.util.List;

public record UnenrollStudentResource(Long userId, List<Long> classroomIds) {
}
