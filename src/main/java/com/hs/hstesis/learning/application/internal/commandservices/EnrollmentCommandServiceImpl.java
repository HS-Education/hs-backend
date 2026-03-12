package com.hs.hstesis.learning.application.internal.commandservices;

import com.hs.hstesis.iam.infrastructure.persistance.jpa.UserRepository;
import com.hs.hstesis.learning.domain.exceptions.EnrollmentNotFoundException;
import com.hs.hstesis.learning.domain.exceptions.UserNotFoundException;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.commands.AssignTeacherToClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.EnrollStudentsToAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.commands.UnassignTeacherFromClassroomsCommand;
import com.hs.hstesis.learning.domain.model.commands.UnenrollUserCommand;
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
//        var classrooms = classroomRepository.findAllBySectionAcademicLevelIdAndAcademicYearId(
//                command.academicLevelId(), command.academicYearId());
//
//        var students = userRepository.findAllById(command.studentIds());
//
//        for (var student : students) {
//            for (var classroom : classrooms) {
//                if (!enrollmentRepository.existsByUserIdAndClassroomId(student.getId(), classroom.getId())) {
//                    var enrollment = new Enrollment(student, classroom, "STUDENT");
//                    enrollmentRepository.save(enrollment);
//                }
//            }
//        }
    }

    @Override
    @Transactional
    public void handle(AssignTeacherToClassroomsCommand command) {
        var teacher = userRepository.findById(command.teacherId())
                .orElseThrow(() -> new UserNotFoundException(command.teacherId()));

        var classrooms = classroomRepository.findAllById(command.classroomIds());

        for (var classroom : classrooms) {
            if (!enrollmentRepository.existsByUserIdAndClassroomId(teacher.getId(), classroom.getId())) {
                var enrollment = new Enrollment(teacher, classroom, "TEACHER");
                enrollmentRepository.save(enrollment);
            }
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
