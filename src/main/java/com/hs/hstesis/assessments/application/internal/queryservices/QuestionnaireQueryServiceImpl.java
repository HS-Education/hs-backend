package com.hs.hstesis.assessments.application.internal.queryservices;

import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionnaireInstancesByStudentIdQuery;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionsByQuestionnaireInstanceIdQuery;
import com.hs.hstesis.assessments.domain.services.QuestionnaireQueryService;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionnaireQueryServiceImpl implements QuestionnaireQueryService {

    private final QuestionnaireInstanceRepository questionnaireInstanceRepository;
    private final QuestionRepository questionRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository;
    private final com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService externalLearningService;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository;

    public QuestionnaireQueryServiceImpl(
            QuestionnaireInstanceRepository questionnaireInstanceRepository, 
            QuestionRepository questionRepository,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository,
            com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService externalLearningService,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository) {
        this.questionnaireInstanceRepository = questionnaireInstanceRepository;
        this.questionRepository = questionRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.externalLearningService = externalLearningService;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public List<QuestionnaireInstance> handle(GetQuestionnaireInstancesByStudentIdQuery query) {
        return questionnaireInstanceRepository.findAllByStudentId(query.studentId());
    }

    @Override
    public List<Question> handle(GetQuestionsByQuestionnaireInstanceIdQuery query) {
        return questionRepository.findAllByQuestionnaireInstanceId(query.questionnaireInstanceId());
    }

    @Override
    public List<com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource> handle(com.hs.hstesis.assessments.domain.model.queries.GetAvailableQuestionnairesQuery query) {
        var studentId = query.studentId();
        
        var courseIds = externalLearningService.getEnrolledCourseIds(studentId);
        if (courseIds.isEmpty()) {
            return java.util.List.of();
        }

        var baseQuestionnaires = questionnaireRepository.findAll().stream()
                .filter(q -> courseIds.contains(q.getCourseId()))
                .filter(q -> q.getTargetStudentId() == null || q.getTargetStudentId().equals(studentId))
                .collect(java.util.stream.Collectors.toList());

        var studentInstances = questionnaireInstanceRepository.findAllByStudentId(studentId);

        return baseQuestionnaires.stream().map(baseQ -> {
            var allInstances = studentInstances.stream()
                    .filter(i -> i.getQuestionnaire().getId().equals(baseQ.getId()))
                    .collect(java.util.stream.Collectors.toList());

            String status = "PENDING";
            Long activeInstanceId = null;
            int completedAttempts = 0;
            java.util.List<com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireAttemptResource> pastAttempts = new java.util.ArrayList<>();
            
            for (var instance : allInstances) {
                var submission = submissionRepository.findByQuestionnaireInstanceIdAndStudentId(instance.getId(), studentId);
                if (submission.isPresent()) {
                    completedAttempts++;
                    pastAttempts.add(new com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireAttemptResource(
                            instance.getId(),
                            submission.get().getScore(),
                            submission.get().getSubmittedAt()
                    ));
                } else {
                    // There is an unfinished instance
                    status = "STARTED";
                    activeInstanceId = instance.getId();
                }
            }
            
            if (status.equals("PENDING") && completedAttempts > 0) {
                if (completedAttempts >= baseQ.getAllowedAttempts()) {
                    status = "COMPLETED";
                } else {
                    // Can still start another attempt, but we can call it RETRY so it's not confused with the first pending attempt
                    status = "RETRY"; 
                }
            }
            
            int attemptsLeft = baseQ.getAllowedAttempts() - completedAttempts;
            if (status.equals("STARTED")) {
                attemptsLeft--; // The current started one counts towards limits usually, but it's already created.
            }
            if (attemptsLeft < 0) attemptsLeft = 0;

            return new com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource(
                    baseQ.getId(),
                    baseQ.getCourseId(),
                    baseQ.getGradingPeriodId(),
                    baseQ.getWeekNumber(),
                    status,
                    activeInstanceId,
                    attemptsLeft,
                    baseQ.getAllowedAttempts(),
                    baseQ.getQuestionsPerAttempt(),
                    baseQ.getType().name(),
                    pastAttempts,
                    baseQ.getCreatedAt() != null ? baseQ.getCreatedAt().toString() : ""
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.Optional<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetSubmissionByInstanceIdQuery query) {
        return submissionRepository.findByQuestionnaireInstanceIdAndStudentId(query.questionnaireInstanceId(), query.studentId());
    }

    @Override
    public List<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetAllQuestionnaireSubmissionsQuery query) {
        return submissionRepository.findAll();
    }
}
