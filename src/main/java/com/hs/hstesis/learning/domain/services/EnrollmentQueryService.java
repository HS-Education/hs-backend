package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Enrollment;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomMembersQuery;

import java.util.List;

public interface EnrollmentQueryService {
    List<Enrollment> handle(GetClassroomMembersQuery query);
}
