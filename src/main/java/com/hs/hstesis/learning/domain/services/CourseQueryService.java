package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.entities.Topic;
import com.hs.hstesis.learning.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface CourseQueryService {
    Optional<Course> handle(GetCourseByIdQuery query);
    List<Course> handle(GetAllCoursesQuery query);
    List<Course> handle(GetCoursesByAreaIdQuery query);
    List<Topic> handle(GetTopicsByCourseIdQuery query);
    boolean handle(ExistsTopicInCourseQuery query);
    boolean handle(ExistsCourseForCoordinatorQuery query);
}
