package com.hs.hstesis.repo.application.internal.outboundservices.acl;

import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import com.hs.hstesis.repo.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.repo.domain.model.valueobjects.UserEnrollmentContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("repoExternalLearningService")
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

    public boolean existsCourse(Long courseId) {
        return learningContextFacade.existsCourse(courseId);
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

    public List<Long> getEnrolledCourseIds(Long userId) {
        return learningContextFacade.getEnrolledCourseIds(userId);
    }

    public Optional<com.hs.hstesis.learning.interfaces.acl.dto.GradingPeriodData> getGradingPeriodByCourseAndBimester(Long courseId, String bimester) {
        return learningContextFacade.getGradingPeriodByCourseAndBimester(courseId, bimester);
    }
}
