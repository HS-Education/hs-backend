package com.hs.hstesis.assessments.interfaces.acl;

import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentsContextFacade {

    private final QuestionnaireSubmissionRepository questionnaireSubmissionRepository;

    public AssessmentsContextFacade(QuestionnaireSubmissionRepository questionnaireSubmissionRepository) {
        this.questionnaireSubmissionRepository = questionnaireSubmissionRepository;
    }

    public List<QuestionnaireSubmission> getSubmissionsByStudentId(Long studentId) {
        return questionnaireSubmissionRepository.findByStudentId(studentId);
    }
}
