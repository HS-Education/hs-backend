package com.hs.hstesis.learning.application.internal.queryservices;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.learning.application.querymodels.EnrollmentQueryModel;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import com.hs.hstesis.learning.infrastructure.jpa.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentQueryServiceImpl implements EnrollmentQueryService {
    private final EnrollmentRepository enrollmentRepository;
    private final IamContextFacade  iamContextFacade;

    public EnrollmentQueryServiceImpl(EnrollmentRepository enrollmentRepository,
                                      IamContextFacade iamContextFacade) {
        this.enrollmentRepository = enrollmentRepository;
        this.iamContextFacade = iamContextFacade;
    }

    @Override
    public List<EnrollmentQueryModel> handle(GetClassroomMembersQuery query) {
        var enrollments = enrollmentRepository.findAllByClassroomId(query.classroomId());

        return enrollments.stream()
                .map(e -> {
                    String name = iamContextFacade.fetchUserNameById(e.getUserId()).orElse("Unknown");
                    return new EnrollmentQueryModel(
                            e.getId(),
                            e.getUserId(),
                            name,
                            e.getRoleInClassroom()
                    );
                }).toList();
    }
}
