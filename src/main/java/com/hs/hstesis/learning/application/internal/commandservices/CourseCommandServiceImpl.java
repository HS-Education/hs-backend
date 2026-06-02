package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AreaNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.CourseNameAlreadyException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import com.hs.hstesis.learning.domain.model.commands.*;
import com.hs.hstesis.learning.domain.services.AcademicYearStateValidator;
import com.hs.hstesis.learning.domain.services.CourseCommandService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.AreaRepository;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.CourseRepository;
import com.hs.hstesis.shared.domain.model.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CourseCommandServiceImpl implements CourseCommandService {
    private final CourseRepository courseRepository;
    private final AreaRepository areaRepository;
    private final com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository gradingPeriodRepository;
    private final AcademicYearStateValidator yearValidator;

    public CourseCommandServiceImpl(CourseRepository courseRepository,
                                    AreaRepository areaRepository,
                                    com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.GradingPeriodRepository gradingPeriodRepository,
                                    AcademicYearStateValidator yearValidator) {
        this.courseRepository = courseRepository;
        this.areaRepository = areaRepository;
        this.gradingPeriodRepository = gradingPeriodRepository;
        this.yearValidator = yearValidator;
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
                .orElseThrow(() -> new CourseNotFoundException(command.id()));

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
        yearValidator.validateCurrentYearIsNotActive();

        if (!courseRepository.existsById(command.id())) {
            throw new CourseNotFoundException(command.id());
        }

        courseRepository.deleteById(command.id());
    }

    @Override
    @Transactional
    public void handle(AddTopicCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(command.courseId()));

        var gradingPeriod = gradingPeriodRepository.findById(command.gradingPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Grading period not found"));

        course.addTopic(command.name(), gradingPeriod);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void handle(ReorderTopicsCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(command.courseId()));

        course.reorderTopics(command.topics(), gradingPeriodRepository);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void handle(RemoveTopicCommand command) {
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(command.courseId()));

        course.removeTopic(command.topicId());
        courseRepository.save(course);
    }
}
