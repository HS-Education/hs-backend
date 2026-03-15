package com.hs.hstesis.learning.interfaces.acl;

import com.hs.hstesis.learning.domain.model.queries.ExistsAreaByCoordinatorIdQuery;
import com.hs.hstesis.learning.domain.model.queries.ExistsEnrollmentByUserIdAndRoleQuery;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import org.springframework.stereotype.Component;

@Component
public class LearningContextFacade {
    private final AreaQueryService areaQueryService;
    private final EnrollmentQueryService enrollmentQueryService;

    public LearningContextFacade(AreaQueryService areaQueryService, EnrollmentQueryService enrollmentQueryService) {
        this.areaQueryService = areaQueryService;
        this.enrollmentQueryService = enrollmentQueryService;
    }

    public boolean isCoordinatorAssignedToAnyArea(Long coordinatorId) {
        if (coordinatorId == null) return false;
        return areaQueryService.handle(new ExistsAreaByCoordinatorIdQuery(coordinatorId));
    }

    public boolean isTeacherAssignedToAnyClassroom(Long teacherId) {
        if (teacherId == null) return false;
        return enrollmentQueryService.handle(new ExistsEnrollmentByUserIdAndRoleQuery(teacherId, "TEACHER"));
    }
}
