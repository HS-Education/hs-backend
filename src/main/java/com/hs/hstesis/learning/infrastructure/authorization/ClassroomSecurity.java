package com.hs.hstesis.learning.infrastructure.authorization;

import com.hs.hstesis.learning.application.internal.outboundservices.acl.ExternalIamService;
import com.hs.hstesis.learning.domain.model.queries.ExistsEnrollmentByUserIdAndClassroomIdQuery;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import org.springframework.stereotype.Component;

@Component("classroomSecurity")
public class ClassroomSecurity {
    private final EnrollmentQueryService enrollmentQueryService;
    private final ExternalIamService externalIamService;

    public ClassroomSecurity(EnrollmentQueryService enrollmentQueryService,
                             ExternalIamService externalIamService) {
        this.enrollmentQueryService = enrollmentQueryService;
        this.externalIamService = externalIamService;
    }

    public boolean isMember(Long classroomId) {
        Long currentUserId = externalIamService.getAuthenticatedUserId();
        return enrollmentQueryService.handle(new ExistsEnrollmentByUserIdAndClassroomIdQuery(currentUserId, classroomId));
    }
}
