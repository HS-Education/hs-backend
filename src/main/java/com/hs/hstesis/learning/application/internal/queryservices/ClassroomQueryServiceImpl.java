package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.queries.*;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.ClassroomRepository;
import com.hs.hstesis.learning.infrastructure.jpa.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassroomQueryServiceImpl implements ClassroomQueryService {
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ClassroomQueryServiceImpl(ClassroomRepository classroomRepository,
                                     EnrollmentRepository enrollmentRepository) {
        this.classroomRepository = classroomRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public Optional<Classroom> handle(GetClassroomByIdQuery query) {
        return classroomRepository.findById(query.id());
    }

    @Override
    public List<Classroom> handle(GetClassroomsByUserIdQuery query) {
        return enrollmentRepository.findAllByUserId(query.userId())
                .stream()
                .map(Enrollment::getClassroom)
                .toList();
    }
}