package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.AcademicLevelNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.CourseAlreadyInStudyPlanException;
import com.hs.hstesis.learning.domain.exceptions.CourseNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.StudyPlanEntryNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.commands.RemoveCourseFromStudyPlanCommand;
import com.hs.hstesis.learning.domain.services.StudyPlanCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.AcademicLevelRepository;
import com.hs.hstesis.learning.infrastructure.jpa.CourseRepository;
import com.hs.hstesis.learning.infrastructure.jpa.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanCommandServiceImpl implements StudyPlanCommandService {
    private final StudyPlanRepository studyPlanRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final CourseRepository courseRepository;

    public StudyPlanCommandServiceImpl(StudyPlanRepository studyPlanRepository,
                                       AcademicLevelRepository academicLevelRepository,
                                       CourseRepository courseRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.academicLevelRepository = academicLevelRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public Long handle(AddCourseToStudyPlanCommand command) {
        var academicLevel = academicLevelRepository.findById(command.academicLevelId())
                .orElseThrow(() -> new AcademicLevelNotFoundException(command.academicLevelId()));

        var course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new CourseNotFoundException(command.courseId()));

        if (studyPlanRepository.existsByAcademicLevelIdAndCourseId(academicLevel.getId(), course.getId())) {
            throw new CourseAlreadyInStudyPlanException(course.getName(), academicLevel.getName());
        }

        var studyPlan = new StudyPlan(academicLevel, course);
        studyPlanRepository.save(studyPlan);
        return studyPlan.getId();
    }

    @Override
    @Transactional
    public void handle(RemoveCourseFromStudyPlanCommand command) {
        var studyPlan = studyPlanRepository
                .findByAcademicLevelIdAndCourseId(command.academicLevelId(), command.courseId())
                .orElseThrow(() -> new StudyPlanEntryNotFoundException(
                        command.academicLevelId(), command.courseId()));
        studyPlanRepository.delete(studyPlan);
    }
}