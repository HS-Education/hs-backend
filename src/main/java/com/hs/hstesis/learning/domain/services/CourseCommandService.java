package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.commands.*;

import java.util.Optional;

public interface CourseCommandService {
    Long handle(CreateCourseCommand command);
    Optional<Course> handle(UpdateCourseCommand command);
    void handle(DeleteCourseCommand command);
    void handle(AddTopicCommand command);
    void handle(ReorderTopicsCommand command);
    void handle(RemoveTopicCommand command);
}
