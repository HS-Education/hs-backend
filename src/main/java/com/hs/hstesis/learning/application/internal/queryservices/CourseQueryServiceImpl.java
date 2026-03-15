package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.queries.GetAllCoursesQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCourseByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetCoursesByAreaIdQuery;
import com.hs.hstesis.learning.domain.services.CourseQueryService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseQueryServiceImpl implements CourseQueryService {
    private final CourseRepository courseRepository;

    public CourseQueryServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public Optional<Course> handle(GetCourseByIdQuery query) {
        return courseRepository.findById(query.id());
    }

    @Override
    public List<Course> handle(GetAllCoursesQuery query) {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> handle(GetCoursesByAreaIdQuery query) {
        return courseRepository.findAllByAreaId(query.areaId());
    }
}
