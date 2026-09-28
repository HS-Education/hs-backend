package com.hs.hstesis.assessments.interfaces.acl;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AssessmentsContextFacade {

    private final QuestionnaireSubmissionRepository questionnaireSubmissionRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository questionnaireInstanceRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.RemedialTrackingRepository remedialTrackingRepository;

    public AssessmentsContextFacade(
            QuestionnaireSubmissionRepository questionnaireSubmissionRepository, 
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository questionnaireInstanceRepository,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.RemedialTrackingRepository remedialTrackingRepository) {
        this.questionnaireSubmissionRepository = questionnaireSubmissionRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.questionnaireInstanceRepository = questionnaireInstanceRepository;
        this.remedialTrackingRepository = remedialTrackingRepository;
    }

    public List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto> getSubmissionsByStudentId(Long studentId) {
        return questionnaireSubmissionRepository.findByStudentId(studentId).stream()
                .map(sub -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto(
                        sub.getId(),
                        sub.getQuestionnaireInstance().getQuestionnaire().getId(),
                        sub.getStudentId(),
                        sub.getScore(),
                        sub.getSubmittedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireDto> getQuestionnairesByCourseAndPeriod(Long courseId, Long gradingPeriodId) {
        return questionnaireRepository.findAllByCourseIdAndGradingPeriodId(courseId, gradingPeriodId).stream()
                .map(q -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireDto(
                        q.getId(),
                        q.getCourseId(),
                        q.getGradingPeriodId(),
                        q.getWeekNumber(),
                        q.getStatus().name()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.Optional<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireDto> getQuestionnaireById(Long questionnaireId) {
        return questionnaireRepository.findById(questionnaireId)
                .map(q -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireDto(
                        q.getId(),
                        q.getCourseId(),
                        q.getGradingPeriodId(),
                        q.getWeekNumber(),
                        q.getStatus().name()
                ));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireProgressDto> getCourseProgress(
            Long courseId, Set<Long> classroomStudentIds) {
        var submissionsByQuestionnaire = (classroomStudentIds.isEmpty()
                ? List.<QuestionnaireSubmission>of()
                : questionnaireSubmissionRepository.findByQuestionnaireInstance_Questionnaire_CourseIdAndStudentIdIn(
                        courseId, classroomStudentIds)).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        submission -> submission.getQuestionnaireInstance().getQuestionnaire().getId()));

        return questionnaireRepository.findAllByCourseId(courseId).stream()
                .filter(questionnaire -> questionnaire.getStatus() !=
                        com.hs.hstesis.assessments.domain.model.valueobjects.QuestionnaireStatus.DRAFT
                        || submissionsByQuestionnaire.containsKey(questionnaire.getId()))
                .sorted(java.util.Comparator.comparing(
                        com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire::getGradingPeriodId)
                        .thenComparing(com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire::getWeekNumber))
                .map(questionnaire -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireProgressDto(
                        questionnaire.getId(), questionnaire.getGradingPeriodId(), questionnaire.getWeekNumber(),
                        questionnaire.getType() == null ? "NORMAL" : questionnaire.getType().name(),
                        questionnaire.getStatus().name(),
                        submissionsByQuestionnaire.getOrDefault(questionnaire.getId(), List.of()).stream()
                                .map(submission -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireProgressDto.Submission(
                                        submission.getStudentId(), submission.getScore(), submission.getSubmittedAt(),
                                        submission.getAnswers().stream()
                                                .map(answer -> new com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireProgressDto.Answer(
                                                        answer.getQuestion().getId(), answer.getQuestion().getTopicId(),
                                                        answer.getQuestion().getText(), answer.getIsCorrect(),
                                                        answer.getQuestion().getIsRemedial()))
                                                .toList()))
                                .toList()))
                .toList();
    }

    public List<com.hs.hstesis.assessments.interfaces.acl.dto.RemedialTrackingDto> getRemedialTrackingsByStudentIdAndCourseId(Long studentId, Long courseId) {
        return remedialTrackingRepository.findAllByStudentIdAndCourseId(studentId, courseId).stream()
                .map(t -> new com.hs.hstesis.assessments.interfaces.acl.dto.RemedialTrackingDto(
                        t.getStudentId(), t.getCourseId(), t.getWeakTopicId(),
                        t.getLastRemedialScore(), t.getIsResolved()))
                .toList();
    }

    public boolean hasActiveQuiz(Long studentId) {
        var instances = questionnaireInstanceRepository.findAllByStudentId(studentId);
        for (var instance : instances) {
            var submission = questionnaireSubmissionRepository.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(instance.getId(), studentId);
            if (submission.isEmpty()) {
                return true; // There is an instance without submission -> active quiz
            }
        }
        return false;
    }
}
