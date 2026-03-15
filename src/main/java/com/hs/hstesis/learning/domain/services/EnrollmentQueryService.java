package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.application.querymodels.EnrollmentQueryModel;
import com.hs.hstesis.learning.domain.model.queries.ExistsEnrollmentByUserIdAndClassroomIdQuery;
import com.hs.hstesis.learning.domain.model.queries.ExistsEnrollmentByUserIdAndRoleQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;

import java.util.List;

public interface EnrollmentQueryService {
    List<EnrollmentQueryModel> handle(GetClassroomMembersQuery query);
    boolean handle(ExistsEnrollmentByUserIdAndRoleQuery query);
    boolean handle(ExistsEnrollmentByUserIdAndClassroomIdQuery query);
}
