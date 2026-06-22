package com.hs.hstesis.assessments.interfaces.acl;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentsContextFacade {

    private final QuestionnaireSubmissionRepository questionnaireSubmissionRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository questionnaireInstanceRepository;

    public AssessmentsContextFacade(
            QuestionnaireSubmissionRepository questionnaireSubmissionRepository, 
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository questionnaireRepository,
            com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository questionnaireInstanceRepository) {
        this.questionnaireSubmissionRepository = questionnaireSubmissionRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.questionnaireInstanceRepository = questionnaireInstanceRepository;
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
