package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AreaNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.CourseNameAlreadyExistsInAreaException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateCourseCommand;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {
    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;

    public CourseCommandServiceImpl(CourseRepository courseRepository, AreaRepository areaRepository) {
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    public Long handle(CreateCourseCommand command){
        var area = areaRepository.findById(command.areaId()).orElseThrow(()-> new AreaNotFoundException(command.areaId()));

        if (courseRepository.existsByNameAndAreaId(command.name(), area.getId())) {
            throw new CourseNameAlreadyExistsInAreaException(command.name(), area.getName());
        }

        var course = new Course(command, area);
        courseRepository.save(course);
        return course.getId();
    }

    @Override
    public Optional<Course> handle(UpdateCourseCommand command){
        var course = courseRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));

        if (courseRepository.existsByNameAndAreaId(command.name(), course.getArea().getId())) {
            throw new CourseNameAlreadyExistsInAreaException(command.name(), course.getArea().getName());
        }

        course.update(command);
        courseRepository.save(course);
        return Optional.of(course);
    }

    @Override
    public void handle(DeleteCourseCommand command){
        if (!courseRepository.existsById(command.id())) {
            throw new CourseNotFoundException(command.id());
        }
        courseRepository.deleteById(command.id());
    }
}
