package com.hs.hstesis.assessments.application.internal.queryservices;

import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.domain.model.entities.RemedialTracking;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionnaireInstancesByStudentIdQuery;
import com.hs.hstesis.assessments.domain.model.queries.GetQuestionsByQuestionnaireInstanceIdQuery;
import com.hs.hstesis.assessments.domain.services.QuestionnaireQueryService;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.RemedialTrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionnaireQueryServiceImpl implements QuestionnaireQueryService {

    private final QuestionnaireInstanceRepository questionnaireInstanceRepository;
    private final QuestionRepository questionRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository;
    private final com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService externalLearningService;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository;
    private final RemedialTrackingRepository remedialTrackingRepository;

    public QuestionnaireQueryServiceImpl(
            QuestionnaireInstanceRepository questionnaireInstanceRepository, 
            QuestionRepository questionRepository,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository,
            com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService externalLearningService,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository,
            RemedialTrackingRepository remedialTrackingRepository) {
        this.questionnaireInstanceRepository = questionnaireInstanceRepository;
        this.questionRepository = questionRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.externalLearningService = externalLearningService;
        this.submissionRepository = submissionRepository;
        this.remedialTrackingRepository = remedialTrackingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionnaireInstance> handle(GetQuestionnaireInstancesByStudentIdQuery query) {
        return questionnaireInstanceRepository.findAllByStudentId(query.studentId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> handle(GetQuestionsByQuestionnaireInstanceIdQuery query) {
        return questionRepository.findAllByQuestionnaireInstanceId(query.questionnaireInstanceId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource> handle(com.hs.hstesis.assessments.domain.model.queries.GetAvailableQuestionnairesQuery query) {
        var studentId = query.studentId();
        
        var enrolledCourseIds = externalLearningService.getEnrolledCourseIds(studentId);
        var coordinatedCourseIds = externalLearningService.getCoordinatedCourseIds(studentId);
        var visibleCourseIds = new java.util.HashSet<Long>(enrolledCourseIds);
        visibleCourseIds.addAll(coordinatedCourseIds);
        if (visibleCourseIds.isEmpty()) {
            return java.util.List.of();
        }

        var baseQuestionnaires = questionnaireRepository.findAll().stream()
                .filter(q -> visibleCourseIds.contains(q.getCourseId()))
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
            int effectiveQuestions = baseQ.getQuestionsPerAttempt() != null ? baseQ.getQuestionsPerAttempt() : 5;
            
            for (var instance : allInstances) {
                var submission = submissionRepository.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(instance.getId(), studentId);
                int instanceQuestions = questionRepository.findAllByQuestionnaireInstanceId(instance.getId()).size();
                if (instanceQuestions > 0) {
                    effectiveQuestions = instanceQuestions;
                }

                if (submission.isPresent()) {
                    completedAttempts++;
                    pastAttempts.add(new com.hs.hstesis.assessments.interfaces.rest.resources.QuestionnaireAttemptResource(
                            instance.getId(),
                            submission.get().getScore(),
                            submission.get().getSubmittedAt(),
                            instanceQuestions > 0 ? instanceQuestions : effectiveQuestions
                    ));
                } else {
                    // There is an unfinished instance
                    status = "STARTED";
                    activeInstanceId = instance.getId();
                }
            }

            if (activeInstanceId == null && completedAttempts == 0) {
                try {
                    var pendingRemedials = remedialTrackingRepository.findAllByCourseIdAndIsResolvedFalse(baseQ.getCourseId()).stream()
                            .filter(r -> r.getStudentId().equals(studentId))
                            .mapToInt(RemedialTracking::questionsToAssign)
                            .sum();
                    effectiveQuestions += pendingRemedials;
                } catch (Exception ignored) {
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
                    effectiveQuestions,
                    baseQ.getType().name(),
                    pastAttempts,
                    baseQ.getCreatedAt() != null ? baseQ.getCreatedAt().toString() : "",
                    !enrolledCourseIds.contains(baseQ.getCourseId())
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetSubmissionByInstanceIdQuery query) {
        var opt = submissionRepository.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(query.questionnaireInstanceId(), query.studentId());
        opt.ifPresent(sub -> {
            sub.getAnswers().size(); // Trigger lazy initialization
            for (var ans : sub.getAnswers()) {
                ans.getQuestion().getOptions().size(); // Trigger lazy initialization of question options if needed
            }
        });
        return opt;
    }

    @Override
    public List<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetAllQuestionnaireSubmissionsQuery query) {
        return submissionRepository.findAll();
    }
}
