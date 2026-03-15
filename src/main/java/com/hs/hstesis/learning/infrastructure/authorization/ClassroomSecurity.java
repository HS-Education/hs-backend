package com.hs.hstesis.learning.infrastructure.authorization;

import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.learning.domain.model.queries.ExistsEnrollmentByUserIdAndClassroomIdQuery;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import org.springframework.stereotype.Component;

@Component("classroomSecurity")
public class ClassroomSecurity {
    private final IamContextFacade iamContextFacade;
    private final EnrollmentQueryService enrollmentQueryService;

    public ClassroomSecurity(IamContextFacade iamContextFacade, EnrollmentQueryService enrollmentQueryService) {
        this.iamContextFacade = iamContextFacade;
        this.enrollmentQueryService = enrollmentQueryService;
    }

    public boolean isMember(Long classroomId) {
        Long currentUserId = iamContextFacade.getAuthenticatedUserId();
        return enrollmentQueryService.handle(new ExistsEnrollmentByUserIdAndClassroomIdQuery(currentUserId, classroomId));
    }
}
