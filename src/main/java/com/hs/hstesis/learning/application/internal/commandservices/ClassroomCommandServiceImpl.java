package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;
import com.hs.hstesis.learning.domain.services.ClassroomCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomCommandServiceImpl implements ClassroomCommandService {
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomRepository classroomRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyPlanRepository studyPlanRepository;

    public ClassroomCommandServiceImpl(SectionRepository sectionRepository,
                                       AcademicYearRepository academicYearRepository,
                                       ClassroomRepository classroomRepository,
                                       AcademicLevelRepository academicLevelRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       StudyPlanRepository studyPlanRepository) {
        this.sectionRepository = sectionRepository;
        this.academicYearRepository = academicYearRepository;
        this.classroomRepository = classroomRepository;
        this.academicLevelRepository = academicLevelRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Override
    @Transactional
    public int handle(GenerateClassroomsCommand command) {

        var activeYear = academicYearRepository.findByIsActiveTrue()
                .orElseThrow(NoActiveAcademicYearException::new);

        var allLevels = academicLevelRepository.findAll();

        if (allLevels.isEmpty()) {
            throw new NoAcademicLevelsFoundException();
        }

        int classroomsCreated = 0;

        for (var level : allLevels) {
            var studyPlanEntries = studyPlanRepository.findAllByAcademicLevelId(level.getId());
            if (studyPlanEntries.isEmpty()) continue;

            var sections = sectionRepository.findAllByAcademicLevelId(level.getId());
            if (sections.isEmpty()) continue;

            for (var section : sections) {
                for (var entry : studyPlanEntries) {
                    var course = entry.getCourse();

                    if (!classroomRepository.existsByCourseIdAndSectionIdAndAcademicYearId(
                            course.getId(), section.getId(), activeYear.getId())) {

                        var classroom = new Classroom(course, section, activeYear);
                        classroomRepository.save(classroom);
                        classroomsCreated++;
                    }
                }
            }
        }
        return classroomsCreated;
    }

    @Override
    @Transactional
    public void handle(DeleteClassroomCommand command) {
        var classroom = classroomRepository.findById(command.id())
                .orElseThrow(() -> new ClassroomNotFoundException(command.id()));

        if (classroom.getIsActive()) {
            throw new CannotDeleteActiveClassroomException(
                    classroom.getCourse().getName(),
                    classroom.getSection().getName()
            );
        }

        if (!classroom.getAcademicYear().getIsActive()) {
            throw new CannotDeleteHistoricalDataException(
                    classroom.getCourse().getName(),
                    classroom.getAcademicYear().getYear()
            );
        }

        enrollmentRepository.deleteAllByClassroomId(classroom.getId());

        classroomRepository.delete(classroom);
    }
}
