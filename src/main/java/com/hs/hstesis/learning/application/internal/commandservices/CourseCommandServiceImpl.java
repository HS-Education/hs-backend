package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AreaNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.CourseAlreadyExistsInAreaException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.InvalidCourseNameException;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.EditCourseNameCommand;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import org.springframework.stereotype.Service;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {
    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;

    public CourseCommandServiceImpl(CourseRepository courseRepository, AreaRepository areaRepository) {
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    public void handle(CreateCourseCommand command){
        var area = areaRepository.findById(command.areaId()).orElseThrow(()-> new AreaNotFoundException(command.areaId()));
        if(command.name().length() < 5 || command.name().length() > 20){
            throw new InvalidCourseNameException(command.name());
        }
        if (courseRepository.existsByNameAndAreaId(command.name(), area.getId())) {
            throw new CourseAlreadyExistsInAreaException(command.name(), area.getName());
        }
        var course = new Course(command, area);
        courseRepository.save(course);
    }

    @Override
    public void handle(EditCourseNameCommand command){
        var course = courseRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));
        if(command.newName().length() < 5 || command.newName().length() > 20){
            throw new InvalidCourseNameException(command.newName());
        }
        if (courseRepository.existsByNameAndAreaId(command.newName(), course.getArea().getId())) {
            throw new CourseAlreadyExistsInAreaException(command.newName(), course.getArea().getName());
        }
        course.editName(command);
        courseRepository.save(course);
    }

    @Override
    public void handle(DeleteCourseCommand command){
        if (!courseRepository.existsById(command.id())) {
            throw new CourseNotFoundException(command.id());
        }
        courseRepository.deleteById(command.id());
    }
}
