package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentQueryServiceImpl implements EnrollmentQueryService {
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentQueryServiceImpl(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public List<Enrollment> handle(GetClassroomMembersQuery query){
        return enrollmentRepository.findAllByClassroomId(query.classroomId());
    }
}
