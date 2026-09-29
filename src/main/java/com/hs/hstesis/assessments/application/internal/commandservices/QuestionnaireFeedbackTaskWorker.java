package com.hs.hstesis.assessments.application.internal.commandservices;

import com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository;
import com.hs.hstesis.shared.domain.model.valueobjects.AiBackgroundTaskType;
import com.hs.hstesis.shared.domain.model.valueobjects.QuestionnaireFeedbackStatus;
import com.hs.hstesis.shared.infrastructure.persistence.jpa.repositories.AiBackgroundTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class QuestionnaireFeedbackTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(QuestionnaireFeedbackTaskWorker.class);

    private final AiBackgroundTaskRepository tasks;
    private final QuestionnaireSubmissionRepository submissions;
    private final ExternalAiService externalAiService;

    public QuestionnaireFeedbackTaskWorker(AiBackgroundTaskRepository tasks,
                                           QuestionnaireSubmissionRepository submissions,
                                           ExternalAiService externalAiService) {
        this.tasks = tasks;
        this.submissions = submissions;
        this.externalAiService = externalAiService;
    }

    @Scheduled(fixedDelayString = "${ai.background-tasks.poll-interval-ms:5000}")
    @Transactional
    public void processNextFeedbackTask() {
        var task = tasks.lockNextReadyTask(AiBackgroundTaskType.QUESTIONNAIRE_FEEDBACK.name(), LocalDateTime.now())
                .orElse(null);
        if (task == null) return;

        var submission = submissions.findByIdWithAnswers(task.getSubjectId())
                .orElseThrow(() -> new IllegalStateException("Questionnaire submission missing for AI task."));
        task.markProcessing();
        submission.markFeedbackProcessing();

        try {
            var incorrectAnswers = submission.getAnswers().stream()
                    .filter(answer -> !answer.getIsCorrect())
                    .map(answer -> new ExternalAiService.WrongQuestionFeedbackDto(
                            answer.getQuestion().getId(),
                            "Topic " + answer.getQuestion().getTopicId(),
                            answer.getQuestion().getText(),
                            answer.getQuestion().getOptions().get(answer.getSelectedOptionIndex()),
                            answer.getQuestion().getOptions().get(answer.getQuestion().getCorrectOptionIndex())))
                    .toList();

            var generated = externalAiService.generateFeedback(incorrectAnswers);
            Map<Long, ExternalAiService.QuestionFeedbackResponse> feedbackByQuestion = generated.stream()
                    .collect(Collectors.toMap(ExternalAiService.QuestionFeedbackResponse::questionId,
                            Function.identity(), (first, ignored) -> first));
            var expectedQuestionIds = incorrectAnswers.stream()
                    .map(ExternalAiService.WrongQuestionFeedbackDto::question_id)
                    .collect(Collectors.toSet());
            if (!feedbackByQuestion.keySet().containsAll(expectedQuestionIds)) {
                throw new IllegalStateException("AI returned incomplete questionnaire feedback.");
            }

            for (var answer : submission.getAnswers()) {
                var feedback = feedbackByQuestion.get(answer.getQuestion().getId());
                if (feedback != null) answer.setAiFeedback(feedback.feedback());
            }
            submission.markFeedbackReady();
            task.markCompleted();
        } catch (RuntimeException exception) {
            boolean exhausted = task.markAttemptFailed();
            if (exhausted) submission.markFeedbackFailed();
            else submission.markFeedbackPending();
            log.warn("Questionnaire feedback generation failed; retryable={}", !exhausted);
        }
    }
}
