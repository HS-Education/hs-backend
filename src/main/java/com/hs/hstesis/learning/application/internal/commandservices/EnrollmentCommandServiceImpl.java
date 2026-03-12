package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.commands.AssignTeacherToClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.EnrollStudentsToAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignTeacherFromClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.UnenrollUserCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.domain.services.EnrollmentCommandService;
import com.hs.hstesis.learning.infrastructure.jpa.ClassroomRepository;
import com.hs.hstesis.learning.infrastructure.jpa.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentCommandServiceImpl implements EnrollmentCommandService {
    private final EnrollmentRepository enrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;

    public EnrollmentCommandServiceImpl(EnrollmentRepository enrollmentRepository,
                                        ClassroomRepository classroomRepository,
                                        UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void handle(EnrollStudentsToAcademicLevelCommand command){
        var classrooms = classroomRepository.findAllBySectionEducationLevelAndSectionGradeLevelAndAcademicYearId(
                command.educationLevel(),
                command.gradeLevel(),
                command.academicYearId());

        if (classrooms.isEmpty()) {
            throw new NoClassroomsDefinedException(
                    command.educationLevel(),
                    command.gradeLevel(),
                    command.academicYearId().intValue()
            );
        }

        var students = userRepository.findAllById(command.studentIds());

        for (var student : students) {
            for (var classroom : classrooms) {
                if (classroom.getAcademicYear().getStatus() == AcademicYearStatus.CLOSED) {
                    continue;
                }

                if (!enrollmentRepository.existsByUserIdAndClassroomId(student.getId(), classroom.getId())) {
                    var enrollment = new Enrollment(student.getId(), classroom, "STUDENT");
                    enrollmentRepository.save(enrollment);
                }
            }
        }
    }

    @Override
    @Transactional
    public void handle(AssignTeacherToClassroomsCommand command) {
        var user = userRepository.findById(command.teacherId())
                .orElseThrow(() -> new UserNotFoundException(command.teacherId()));

        boolean isTeacher = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("TEACHER"));
        if (!isTeacher) {
            throw new InvalidUserRoleException(user.getName(), "TEACHER");
        }

        var classrooms = classroomRepository.findAllById(command.classroomIds());
        if (classrooms.isEmpty()) {
            throw new RuntimeException("No classrooms were found with the provided IDs.");
        }

        for (var classroom : classrooms) {
            if (classroom.getAcademicYear().getStatus() == AcademicYearStatus.CLOSED) {
                throw new IllegalStateException("Cannot assign teachers to an archived (CLOSED) academic year.");
            }

            boolean hasTeacher = enrollmentRepository.existsByClassroomIdAndRoleInClassroom(
                    classroom.getId(), "TEACHER");
            if (hasTeacher) {
                boolean isAlreadyAssignedToThisUser = enrollmentRepository.existsByUserIdAndClassroomId(
                        user.getId(), classroom.getId());

                if (!isAlreadyAssignedToThisUser) {
                    throw new TeacherAlreadyAssignedException();
                }
                continue;
            }

            var enrollment = new Enrollment(user.getId(), classroom, "TEACHER");
            enrollmentRepository.save(enrollment);
        }
    }

    @Override
    @Transactional
    public void handle(UnassignTeacherFromClassroomsCommand command) {
        for (Long classroomId : command.classroomIds()) {
            enrollmentRepository.findByUserIdAndClassroomId(command.teacherId(), classroomId)
                    .ifPresent(enrollmentRepository::delete);
        }
    }

    @Override
    @Transactional
    public void handle(UnenrollUserCommand command) {
        var enrollment = enrollmentRepository.findByUserIdAndClassroomId(
                        command.userId(), command.classroomId())
                .orElseThrow(() -> new EnrollmentNotFoundException(command.userId(), command.classroomId()));

        enrollmentRepository.delete(enrollment);
    }
}
