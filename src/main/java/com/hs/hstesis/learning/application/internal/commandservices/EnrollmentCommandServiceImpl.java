package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.learning.domain.exceptions.*;
import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.commands.AssignTeacherToClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.EnrollStudentsToAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignTeacherFromClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.UnenrollStudentFromClassroomsCommand;
import com.hs.hstesis.learning.domain.model.valueobjects.AcademicYearStatus;
import com.hs.hstesis.learning.domain.services.EnrollmentCommandService;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.ClassroomRepository;
import com.hs.hstesis.learning.infrastructure.persistance.jpa.repositories.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EnrollmentCommandServiceImpl implements EnrollmentCommandService {
    private final EnrollmentRepository enrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final IamContextFacade iamContextFacade;

    public EnrollmentCommandServiceImpl(EnrollmentRepository enrollmentRepository,
                                        ClassroomRepository classroomRepository,
                                        IamContextFacade  iamContextFacade) {
        this.enrollmentRepository = enrollmentRepository;
        this.classroomRepository = classroomRepository;
        this.iamContextFacade = iamContextFacade;
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

        Set<Long> uniqueStudentIds = new HashSet<>(command.studentIds());

        var missingUsers = iamContextFacade.getMissingUsers(uniqueStudentIds);
        if (!missingUsers.isEmpty()) {
            throw new UserNotFoundException(missingUsers);
        }

        var invalidUsers = iamContextFacade.getUserNamesWithoutRole(uniqueStudentIds, "STUDENT");
        if (!invalidUsers.isEmpty()) {
            throw new InvalidUserRoleException(invalidUsers.getFirst(), "STUDENT");
        }

        for (Long studentId : command.studentIds()) {
            for (var classroom : classrooms) {
                if (classroom.getAcademicYear().getStatus() == AcademicYearStatus.CLOSED) {
                    continue;
                }

                if (!enrollmentRepository.existsByUserIdAndClassroomId(studentId, classroom.getId())) {
                    var enrollment = new Enrollment(studentId, classroom, "STUDENT");
                    enrollmentRepository.save(enrollment);
                }
            }
        }
    }

    @Override
    @Transactional
    public void handle(AssignTeacherToClassroomsCommand command) {
        String teacherName = iamContextFacade.fetchUserNameById(command.teacherId())
                .orElseThrow(() -> new UserNotFoundException(command.teacherId()));

        if (!iamContextFacade.hasRole(command.teacherId(), "TEACHER")) {
            throw new InvalidUserRoleException(teacherName, "TEACHER");
        }

        var foundClassrooms = classroomRepository.findAllById(command.classroomIds());
        if (foundClassrooms.size() != command.classroomIds().size()) {
            var foundIds = foundClassrooms.stream().map(Classroom::getId).toList();
            var missingIds = command.classroomIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new ClassroomNotFoundException(missingIds);
        }

        for (var classroom : foundClassrooms) {
            if (classroom.getAcademicYear().getStatus() == AcademicYearStatus.CLOSED) {
                throw new CannotDeleteHistoricalDataException();
            }

            boolean hasTeacher = enrollmentRepository.existsByClassroomIdAndRoleInClassroom(
                    classroom.getId(), "TEACHER");
            if (hasTeacher) {
                boolean isAlreadyAssignedToThisUser = enrollmentRepository.existsByUserIdAndClassroomId(
                        command.teacherId(), classroom.getId());

                if (!isAlreadyAssignedToThisUser) {
                    throw new TeacherAlreadyAssignedException();
                }
                continue;
            }

            var enrollment = new Enrollment(command.teacherId(), classroom, "TEACHER");
            enrollmentRepository.save(enrollment);
        }
    }

    @Override
    @Transactional
    public void handle(UnassignTeacherFromClassroomsCommand command) {
        removeUserFromClassrooms(command.teacherId(), "TEACHER", command.classroomIds());
    }

    @Override
    @Transactional
    public void handle(UnenrollStudentFromClassroomsCommand command) {
        removeUserFromClassrooms(command.studentId(), "STUDENT", command.classroomIds());
    }

    private void removeUserFromClassrooms(Long userId, String requiredRole, List<Long> classroomIds) {
        String userName = iamContextFacade.fetchUserNameById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!iamContextFacade.hasRole(userId, requiredRole)) {
            throw new InvalidUserRoleException(userName, requiredRole);
        }

        for (Long classroomId : classroomIds) {
            var enrollment = enrollmentRepository.findByUserIdAndClassroomId(userId, classroomId)
                    .orElseThrow(EnrollmentNotFoundException::new);

            if (enrollment.getClassroom().getAcademicYear().getStatus() == AcademicYearStatus.CLOSED) {
                throw new CannotDeleteHistoricalDataException();
            }

            enrollmentRepository.delete(enrollment);
        }
    }
}
