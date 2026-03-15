package com.hs.hstesis.learning.domain.services;

import com.hs.hstesis.learning.domain.model.aggregates.Classroom;
import com.hs.hstesis.learning.domain.model.queries.*;

import java.util.List;
import java.util.Optional;

public interface ClassroomQueryService {
    Optional<Classroom> handle(GetClassroomByIdQuery query);
    List<Classroom> handle(GetClassroomsByUserIdQuery query);
    List<Classroom> handle(GetAllClassroomsQuery query);
}
