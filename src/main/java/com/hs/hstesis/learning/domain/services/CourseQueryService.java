package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.queries.GetAllCoursesQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCoursesByAreaIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCourseByIdQuery;

import java.util.List;
import java.util.Optional;

public interface CourseQueryService {
    Optional<Course> handle(GetCourseByIdQuery query);
    List<Course> handle(GetAllCoursesQuery query);
    List<Course> handle(GetCoursesByAreaIdQuery query);
}
