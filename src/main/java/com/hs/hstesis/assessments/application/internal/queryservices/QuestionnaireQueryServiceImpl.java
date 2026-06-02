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
                .collect(java.util.stream.Collectors.toList());

        var studentInstances = questionnaireInstanceRepository.findAllByStudentId(studentId);

        return baseQuestionnaires.stream().map(baseQ -> {
            var instanceOpt = studentInstances.stream()
                    .filter(i -> i.getQuestionnaire().getId().equals(baseQ.getId()))
                    .findFirst();

            String status = "PENDING";
            Long instanceId = null;

            if (instanceOpt.isPresent()) {
                instanceId = instanceOpt.get().getId();
                var submission = submissionRepository.findByQuestionnaireInstanceIdAndStudentId(instanceId, studentId);
                if (submission.isPresent()) {
                    status = "COMPLETED";
                } else {
                    status = "STARTED";
                }
            }

            return new com.hs.hstesis.assessments.interfaces.rest.resources.AvailableQuestionnaireResource(
                    baseQ.getId(),
                    baseQ.getCourseId(),
                    baseQ.getGradingPeriodId(),
                    baseQ.getWeekNumber(),
                    status,
                    instanceId
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.Optional<com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission> handle(com.hs.hstesis.assessments.domain.model.queries.GetSubmissionByInstanceIdQuery query) {
        return submissionRepository.findByQuestionnaireInstanceIdAndStudentId(query.questionnaireInstanceId(), query.studentId());
    }
}
