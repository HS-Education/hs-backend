package com.hs.hstesis.achievements.application.internal.queryservices;

import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalAssessmentService;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.TopicPerformance;
import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import com.hs.hstesis.learning.interfaces.acl.dto.ClassroomStudentData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AchievementQueryServiceImplTest {
    @Test
    void classroomAverageUsesOnlyTopicsOfItsCourseAndIncludesStudentsWithoutScores() {
        var assessments = mock(ExternalAssessmentService.class);
        var learning = mock(LearningContextFacade.class);
        var service = spy(new AchievementQueryServiceImpl(assessments, learning));
        when(learning.getStudentsByClassroom(4L)).thenReturn(List.of(
                new ClassroomStudentData(1L, "A"), new ClassroomStudentData(2L, "B")));
        when(learning.getCourseIdByClassroomId(4L)).thenReturn(Optional.of(10L));
        var targetTopic = new TopicPerformance(1L, "target", 1, 16, 80.0, 1L, 10L, List.of());
        var otherCourse = new TopicPerformance(2L, "other", 1, 20, 100.0, 1L, 99L, List.of());
        doReturn(Optional.of(new StudentPerformance(1L, "A", 90.0, List.of(targetTopic, otherCourse))))
                .when(service).handle(new GetStudentPerformanceQuery(1L));
        doReturn(Optional.empty()).when(service).handle(new GetStudentPerformanceQuery(2L));

        var result = service.handle(new GetClassroomPerformanceQuery(4L)).orElseThrow();

        assertThat(result.averageScore()).isEqualTo(40.0);
        assertThat(result.students()).hasSize(2);
        assertThat(result.students().getFirst().topics()).containsExactly(targetTopic);
        assertThat(result.students().get(1).averageScore()).isZero();
    }
}
