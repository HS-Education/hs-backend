package com.hs.hstesis.assessments.application.internal.outboundservices.acl;

import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import com.hs.hstesis.learning.domain.model.entities.Topic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("assessmentsExternalLearningService")
public class ExternalLearningService {
    private final LearningContextFacade learningContextFacade;

    public ExternalLearningService(LearningContextFacade learningContextFacade) {
        this.learningContextFacade = learningContextFacade;
    }

    public Optional<Topic> getTopicByCourseAndGradingPeriodAndOrderIndex(Long courseId, Long gradingPeriodId, Integer orderIndex) {
        return learningContextFacade.getTopicByCourseAndGradingPeriodAndOrderIndex(courseId, gradingPeriodId, orderIndex);
    }

    public List<Long> getEnrolledCourseIds(Long studentId) {
        return learningContextFacade.getEnrolledCourseIds(studentId);
    }

    public List<Long> getStudentIdsByCourseId(Long courseId) {
        return learningContextFacade.getStudentsByCourseId(courseId)
                .stream()
                .map(com.hs.hstesis.learning.interfaces.acl.dto.ClassroomStudentData::userId)
                .toList();
    }
}
