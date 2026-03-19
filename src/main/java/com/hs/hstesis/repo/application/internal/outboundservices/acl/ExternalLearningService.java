package com.hs.hstesis.repo.application.internal.outboundservices.acl;

import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.UserEnrollmentContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExternalLearningService {

    private final LearningContextFacade learningContextFacade;

    public ExternalLearningService(LearningContextFacade learningContextFacade) {
        this.learningContextFacade = learningContextFacade;
    }

    public boolean isCoordinatorAssignedToAnyArea(Long userId) {
        return learningContextFacade.isCoordinatorAssignedToAnyArea(userId);
    }

    public boolean doesTopicBelongToCourse(Long topicId, Long courseId) {
        return learningContextFacade.doesTopicBelongToCourse(topicId, courseId);
    }

    public boolean doesCoordinatorOwnCourse(Long coordinatorId, Long courseId) {
        return learningContextFacade.doesCoordinatorOwnCourse(coordinatorId, courseId);
    }

    public Optional<UserEnrollmentContext> getUserEnrollmentContextByCourse(Long userId, Long courseId) {
        return learningContextFacade
                .getUserEnrollmentDataByCourse(userId, courseId)
                .map(data -> new UserEnrollmentContext(
                        EducationLevel.valueOf(data.level().name()),
                        GradeLevel.valueOf(data.grade().name()),
                        data.courseId()
                ));
    }

}
