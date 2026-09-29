package com.hs.hstesis.assessments.application.internal.commandservices;

import com.hs.hstesis.assessments.application.internal.outboundservices.acl.*;
import com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.assessments.domain.model.commands.*;
import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionDifficulty;
import com.hs.hstesis.shared.domain.model.entities.AiBackgroundTask;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus;
import com.hs.hstesis.shared.infrastructure.persistence.jpa.repositories.AiBackgroundTaskRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
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
    private final AiBackgroundTaskRepository aiTasks = mock(AiBackgroundTaskRepository.class);
    private final QuestionnaireCommandServiceImpl service = new QuestionnaireCommandServiceImpl(
            questionnaires, instances, questions, remediation, submissions, learning, repository, ai, notifications, aiTasks);

    @Test
    void studentCannotSubmitAnotherStudentsInstance() {
        var instance = mock(QuestionnaireInstance.class);
        when(instance.getStudentId()).thenReturn(99L);
        when(instances.findByIdForUpdate(12L)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.handle(new SubmitQuestionnaireCommand(12L, Map.of(), 7L)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(questions, submissions, ai, notifications);
    }

    @Test
    void invalidOptionAndUnknownQuestionAreRejectedBeforeScoring() {
        var instance = mock(QuestionnaireInstance.class);
        when(instance.getStudentId()).thenReturn(7L);
        when(instance.getId()).thenReturn(12L);
        when(instances.findByIdForUpdate(12L)).thenReturn(Optional.of(instance));
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

    @Test
    void submissionReturnsPersistedScoreAndQueuesFeedbackAndInsightWithoutCallingAiInline() {
        var instance = mock(QuestionnaireInstance.class);
        var questionnaire = mock(Questionnaire.class);
        when(instance.getStudentId()).thenReturn(7L);
        when(instance.getId()).thenReturn(12L);
        when(instance.getQuestionnaire()).thenReturn(questionnaire);
        when(questionnaire.getCourseId()).thenReturn(30L);
        when(questionnaire.getWeekNumber()).thenReturn(4);
        when(instances.findByIdForUpdate(12L)).thenReturn(Optional.of(instance));
        when(submissions.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(12L, 7L))
                .thenReturn(Optional.empty());

        var correctOne = question(instance, 1L, 0);
        var incorrect = question(instance, 2L, 0);
        var correctTwo = question(instance, 3L, 0);
        when(questions.findAllByQuestionnaireInstanceId(12L)).thenReturn(List.of(correctOne, incorrect, correctTwo));
        when(remediation.findFirstByStudentIdAndCourseIdAndWeakTopicIdAndIsResolvedFalse(7L, 30L, 90L))
                .thenReturn(Optional.empty());
        when(submissions.saveAndFlush(any(QuestionnaireSubmission.class))).thenAnswer(invocation -> {
            var submission = invocation.getArgument(0, QuestionnaireSubmission.class);
            ReflectionTestUtils.setField(submission, "id", 500L);
            return submission;
        });

        QuestionnaireSubmission submission = service.handle(
                new SubmitQuestionnaireCommand(12L, Map.of(1L, 0, 2L, 1, 3L, 0), 7L));

        assertThat(submission.getId()).isEqualTo(500L);
        assertThat(submission.getScore()).isEqualTo(13);
        assertThat(submission.getAnswers()).hasSize(3);
        assertThat(submission.getFeedbackStatus()).isEqualTo(QuestionnaireFeedbackStatus.PENDING);
        ArgumentCaptor<AiBackgroundTask> taskCaptor = ArgumentCaptor.forClass(AiBackgroundTask.class);
        verify(aiTasks, times(2)).save(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues()).extracting(AiBackgroundTask::getTaskType)
                .containsExactlyInAnyOrder(AiBackgroundTaskType.QUESTIONNAIRE_FEEDBACK,
                        AiBackgroundTaskType.STUDENT_INSIGHT);
        verify(ai, never()).generateFeedback(anyList());
    }

    @Test
    void repeatedSubmissionReturnsExistingResultWithoutDuplicatingBackgroundTasks() {
        var instance = mock(QuestionnaireInstance.class);
        var existing = mock(QuestionnaireSubmission.class);
        when(instance.getStudentId()).thenReturn(7L);
        when(instance.getId()).thenReturn(12L);
        when(instances.findByIdForUpdate(12L)).thenReturn(Optional.of(instance));
        when(submissions.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(12L, 7L))
                .thenReturn(Optional.of(existing));

        assertThat(service.handle(new SubmitQuestionnaireCommand(12L, Map.of(), 7L))).isSameAs(existing);

        verifyNoInteractions(questions, aiTasks, ai);
        verify(submissions, never()).saveAndFlush(any());
    }

    private Question question(QuestionnaireInstance instance, Long id, int correctIndex) {
        var question = mock(Question.class);
        when(question.getId()).thenReturn(id);
        when(question.getTopicId()).thenReturn(90L);
        when(question.getText()).thenReturn("Synthetic question " + id);
        when(question.getOptions()).thenReturn(List.of("Correct", "Incorrect", "Option C", "Option D"));
        when(question.getCorrectOptionIndex()).thenReturn(correctIndex);
        when(question.getIsRemedial()).thenReturn(false);
        when(question.getQuestionnaireInstance()).thenReturn(instance);
        when(question.getDifficulty()).thenReturn(QuestionDifficulty.INTERMEDIATE);
        return question;
    }
}
