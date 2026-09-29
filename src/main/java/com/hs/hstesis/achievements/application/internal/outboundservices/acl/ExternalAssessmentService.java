package com.hs.hstesis.achievements.application.internal.outboundservices.acl;

import com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade;
import com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireDto;
import com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalAssessmentService {

    private final AssessmentsContextFacade assessmentsContextFacade;

    public ExternalAssessmentService(AssessmentsContextFacade assessmentsContextFacade) {
        this.assessmentsContextFacade = assessmentsContextFacade;
    }

    public List<QuestionnaireSubmissionDto> getSubmissionsByStudentId(Long studentId) {
        return assessmentsContextFacade.getSubmissionsByStudentId(studentId);
    }

    public List<QuestionnaireDto> getQuestionnairesByCourseAndPeriod(Long courseId, Long gradingPeriodId) {
        return assessmentsContextFacade.getQuestionnairesByCourseAndPeriod(courseId, gradingPeriodId);
    }

    public java.util.Optional<QuestionnaireDto> getQuestionnaireById(Long questionnaireId) {
        return assessmentsContextFacade.getQuestionnaireById(questionnaireId);
    }

    public List<com.hs.hstesis.assessments.interfaces.acl.dto.RemedialTrackingDto> getRemedialTrackingsByStudentIdAndCourseId(Long studentId, Long courseId) {
        return assessmentsContextFacade.getRemedialTrackingsByStudentIdAndCourseId(studentId, courseId);
    }
}
