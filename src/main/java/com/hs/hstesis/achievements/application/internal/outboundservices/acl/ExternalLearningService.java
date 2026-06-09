package com.hs.hstesis.achievements.application.internal.outboundservices.acl;

import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import com.hs.hstesis.learning.domain.model.aggregates.Course;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("achievementsExternalLearningService")
public class ExternalLearningService {
    private final LearningContextFacade learningContextFacade;

    public ExternalLearningService(LearningContextFacade learningContextFacade) {
        this.learningContextFacade = learningContextFacade;
    }

    public List<Long> getEnrolledCourseIds(Long studentId) {
        return learningContextFacade.getEnrolledCourseIds(studentId);
    }

    public Optional<Long> getCourseIdByClassroomId(Long classroomId) {
        return learningContextFacade.getCourseIdByClassroomId(classroomId);
    }

    public List<Long> getCoursesByAreaId(Long areaId) {
        return learningContextFacade.getCoursesByAreaId(areaId).stream()
                .map(Course::getId)
                .toList();
    }
}
