package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.CourseAlreadyInStudyPlanException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.StudyPlanEntryNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.commands.RemoveCourseFromStudyPlanCommand;
import com.hs.hstesis.learning.domain.services.AcademicYearStateValidator;
import com.hs.hstesis.learning.domain.services.StudyPlanCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import com.hs.hstesis.learning.infrastructure.jpa.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanCommandServiceImpl implements StudyPlanCommandService {
    private final StudyPlanRepository studyPlanRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearStateValidator yearValidator;

    public StudyPlanCommandServiceImpl(StudyPlanRepository studyPlanRepository,
                                       CourseRepository courseRepository,
                                       AcademicYearStateValidator yearValidator) {
        this.studyPlanRepository = studyPlanRepository;
        this.courseRepository = courseRepository;
        this.yearValidator = yearValidator;
    }

    @Override
    @Transactional
    public Long handle(AddCourseToStudyPlanCommand command) {
        var course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(command.courseId()));

        if (studyPlanRepository.existsByEducationLevelAndGradeLevelAndCourseId(command.educationLevel(), command.gradeLevel(), course.getId())) {
            throw new CourseAlreadyInStudyPlanException(course.getName(), command.educationLevel().name() + " " + command.gradeLevel().name()
            );
        }

        var studyPlan = new StudyPlan(command.educationLevel(), command.gradeLevel(), course);
        studyPlanRepository.save(studyPlan);
        return studyPlan.getId();
    }

    @Override
    @Transactional
    public void handle(RemoveCourseFromStudyPlanCommand command) {
        yearValidator.validateCurrentYearIsNotActive();

        if (!studyPlanRepository.existsById(command.id())) {
            throw new StudyPlanEntryNotFoundException(command.id());
        }
        studyPlanRepository.deleteById(command.id());
    }
}