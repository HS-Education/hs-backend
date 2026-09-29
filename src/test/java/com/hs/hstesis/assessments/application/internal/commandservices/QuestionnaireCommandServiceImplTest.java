package com.hs.hstesis.assessments.application.internal.commandservices;

import com.hs.hstesis.assessments.application.internal.outboundservices.acl.*;
import com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.assessments.domain.model.commands.*;
import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionnaireCommandServiceImplTest {
    private final QuestionnaireRepository questionnaires = mock(QuestionnaireRepository.class);
    private final QuestionnaireInstanceRepository instances = mock(QuestionnaireInstanceRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final RemedialTrackingRepository remediation = mock(RemedialTrackingRepository.class);
    private final QuestionnaireSubmissionRepository submissions = mock(QuestionnaireSubmissionRepository.class);
    private final ExternalLearningService learning = mock(ExternalLearningService.class);
    private final ExternalRepoService repository = mock(ExternalRepoService.class);
    private final ExternalAiService ai = mock(ExternalAiService.class);
    private final ExternalNotificationService notifications = mock(ExternalNotificationService.class);
    private final QuestionnaireCommandServiceImpl service = new QuestionnaireCommandServiceImpl(
            questionnaires, instances, questions, remediation, submissions, learning, repository, ai, notifications);

    @Test
    void studentCannotSubmitAnotherStudentsInstance() {
        var instance = mock(QuestionnaireInstance.class);
        when(instance.getStudentId()).thenReturn(99L);
        when(instances.findById(12L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.handle(new SubmitQuestionnaireCommand(12L, Map.of(), 7L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(questions, submissions, ai, notifications);
    }

    @Test
    void invalidOptionAndUnknownQuestionAreRejectedBeforeScoring() {
        var instance = mock(QuestionnaireInstance.class);
        when(instance.getStudentId()).thenReturn(7L);
        when(instance.getId()).thenReturn(12L);
        when(instances.findById(12L)).thenReturn(Optional.of(instance));
        var question = mock(Question.class);
        when(question.getId()).thenReturn(5L);
        when(question.getOptions()).thenReturn(List.of("A", "B", "C", "D"));
        when(questions.findAllByQuestionnaireInstanceId(12L)).thenReturn(List.of(question));

        assertThatThrownBy(() -> service.handle(new SubmitQuestionnaireCommand(12L, Map.of(5L, -1), 7L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.handle(new SubmitQuestionnaireCommand(12L, Map.of(5L, 4), 7L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.handle(new SubmitQuestionnaireCommand(12L, Map.of(999L, 0), 7L)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(submissions, never()).save(any());
        verifyNoInteractions(ai, notifications);
    }

    @Test
    void teacherCannotGenerateForUnassignedCourse() {
        assertThatThrownBy(() -> service.handle(new GenerateQuestionnaireCommand(2L, 3L, 1, 1, 5, 7L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, ai, questionnaires);
    }

    @Test
    void coordinatorCannotGenerateRemedialForOtherCourse() {
        when(learning.getCoordinatedCourseIds(7L)).thenReturn(List.of(2L));
        assertThatThrownBy(() -> service.handle(new GenerateRemedialQuestionnaireCommand(
                9L, 3L, 4L, 1, 5L, 10, 7L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(questions, questionnaires, ai);
    }

    @Test
    void coordinatorCannotGenerateRemedialForUnenrolledStudent() {
        when(learning.getCoordinatedCourseIds(7L)).thenReturn(List.of(3L));
        when(learning.getStudentIdsByCourseId(3L)).thenReturn(List.of(8L));
        assertThatThrownBy(() -> service.handle(new GenerateRemedialQuestionnaireCommand(
                9L, 3L, 4L, 1, 5L, 10, 7L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(questions, questionnaires, ai);
    }
}
