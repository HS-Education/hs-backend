package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AreaNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.CourseNameAlreadyException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.commands.CreateCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteCourseCommand;
import com.hs.hstesis.learning.domain.model.commands.UpdateCourseCommand;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicYearRepository;
import com.hs.hstesis.learning.infrastructure.jpa.AreaRepository;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import com.hs.hstesis.shared.domain.model.util.TextUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {
    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;

    public CourseCommandServiceImpl(CourseRepository courseRepository,
                                    AreaRepository areaRepository) {
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    public Long handle(CreateCourseCommand command){
        var area = areaRepository.findById(command.areaId()).orElseThrow(()-> new AreaNotFoundException(command.areaId()));

        String nameToCreate = TextUtils.normalize(command.name());

        boolean alreadyExists = courseRepository.findAll().stream()
                .anyMatch(c -> TextUtils.normalize(c.getName()).equals(nameToCreate));

        if (alreadyExists) {
            throw new CourseNameAlreadyException(nameToCreate);
        }

        var course = new Course(command, area);
        courseRepository.save(course);
        return course.getId();
    }

    @Override
    public Optional<Course> handle(UpdateCourseCommand command){
        var course = courseRepository.findById(command.id())
                .orElseThrow(() -> new AreaNotFoundException(command.id()));

        if (command.name() != null) {
            String newNormalizedName = TextUtils.normalize(command.name());
            String currentNormalizedName = TextUtils.normalize(course.getName());

            if (!newNormalizedName.equals(currentNormalizedName)) {

                boolean alreadyExists = courseRepository.findAll().stream()
                        .anyMatch(c -> TextUtils.normalize(c.getName()).equals(newNormalizedName));

                if (alreadyExists) {
                    throw new CourseNameAlreadyException(newNormalizedName);
                }
            }
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
