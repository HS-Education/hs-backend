package com.hs.hstesis.iam.application.internal.outboundservices.acl;

import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import org.springframework.stereotype.Service;

@Service
public class ExternalLearningService {
    private final LearningContextFacade learningContextFacade;

    public  ExternalLearningService(LearningContextFacade learningContextFacade) {
        this.learningContextFacade = learningContextFacade;
    }

    public boolean isCoordinatorAssignedToAnyArea(Long userId) {
        return learningContextFacade.isCoordinatorAssignedToAnyArea(userId);
    }

    public boolean isTeacherAssignedToAnyClassroom(Long userId) {
        return learningContextFacade.isTeacherAssignedToAnyClassroom(userId);
    }
}
