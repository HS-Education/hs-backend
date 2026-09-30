package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetClassroomsByUserIdQuery;
import com.hs.hstesis.learning.domain.services.AreaCommandService;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.domain.services.ClassroomCommandService;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmptyCollectionsControllerTest {
    @Test
    void returnsAnEmptyClassroomListForAUserWithoutClassrooms() {
        var queries = mock(ClassroomQueryService.class);
        when(queries.handle(any(GetClassroomsByUserIdQuery.class))).thenReturn(List.of());
        var controller = new ClassroomController(
                mock(ClassroomCommandService.class), queries, mock(EnrollmentQueryService.class));

        var response = controller.getClassrooms(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void returnsAnEmptyAreaListWhenNoAreasExist() {
        var queries = mock(AreaQueryService.class);
        when(queries.handle(any(GetAllAreasQuery.class))).thenReturn(List.of());
        var controller = new AreaController(mock(AreaCommandService.class), queries);

        var response = controller.getAllAreas();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
