package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.StudyPlan;
import com.hs.hstesis.learning.domain.model.commands.DeleteClassroomCommand;
import com.hs.hstesis.learning.domain.model.commands.GenerateClassroomsCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.*;
import com.hs.hstesis.learning.domain.services.ClassroomCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassroomCommandServiceImpl implements ClassroomCommandService {
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyPlanRepository studyPlanRepository;

    public ClassroomCommandServiceImpl(SectionRepository sectionRepository,
                                       AcademicYearRepository academicYearRepository,
                                       ClassroomRepository classroomRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       StudyPlanRepository studyPlanRepository) {
        this.sectionRepository = sectionRepository;
        this.academicYearRepository = academicYearRepository;
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Override
    @Transactional
    public int handle(GenerateClassroomsCommand command) {

        var planningYear = academicYearRepository.findAll().stream()
                .filter(year -> {
                    boolean hasDates = !year.getPeriods().isEmpty() &&
                            year.getPeriods().stream().allMatch(p -> p.getStartDate() != null);
                    return year.getStatus() == AcademicYearStatus.PLANNED && hasDates;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No academic year available for classroom generation. " +
                        "Ensure the year is in PLANNED status and all grading periods are configured."));

        if (planningYear.getStatus() != AcademicYearStatus.PLANNED) {
            throw new ClassroomGenerationDeadlineExceededException();
        }

        var activeStudyPlans = studyPlanRepository.findAll();

        var levelsToGenerate = activeStudyPlans.stream()
                .collect(Collectors.groupingBy(sp ->
                        new AbstractMap.SimpleEntry<>(sp.getEducationLevel(), sp.getGradeLevel())
                ));

        if (levelsToGenerate.isEmpty()) {
            throw new IllegalStateException("No study plans found to generate classrooms.");
        }

        int classroomsCreated = 0;

        for (var entry : levelsToGenerate.entrySet()) {
            EducationLevel eduLevel = entry.getKey().getKey();
            GradeLevel gradeLevel = entry.getKey().getValue();
            List<StudyPlan> coursesInPlan = entry.getValue();

            var sections = sectionRepository.findAllByEducationLevelAndGradeLevel(eduLevel, gradeLevel);

            if (sections.isEmpty()) continue;

            for (var section : sections) {
                for (var studyPlan : coursesInPlan) {
                    var course = studyPlan.getCourse();

                    if (!classroomRepository.existsByCourseIdAndSectionIdAndAcademicYearId(
                            course.getId(), section.getId(), planningYear.getId())) {

                        var classroom = new Classroom(course, section, planningYear);
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

        if (classroom.getStatus() == ClassroomStatus.ACTIVE) {
            throw new CannotDeleteActiveClassroomException();
        } else if (classroom.getStatus() == ClassroomStatus.ARCHIVED) {
            throw new CannotDeleteHistoricalDataException();
        }

        enrollmentRepository.deleteAllByClassroomId(classroom.getId());
        classroomRepository.delete(classroom);
    }
}
