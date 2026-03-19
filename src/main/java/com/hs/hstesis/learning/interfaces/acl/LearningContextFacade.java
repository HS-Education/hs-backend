package com.hs.hstesis.learning.interfaces.acl;

import com.hs.hstesis.learning.domain.model.queries.*;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.domain.services.ClassroomQueryService;
import com.hs.hstesis.learning.domain.services.CourseQueryService;
import com.hs.hstesis.learning.domain.services.EnrollmentQueryService;
import com.hs.hstesis.learning.interfaces.acl.dto.UserEnrollmentData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LearningContextFacade {
    private final AreaQueryService areaQueryService;
    private final EnrollmentQueryService enrollmentQueryService;
    private final CourseQueryService courseQueryService;
    private final ClassroomQueryService classroomQueryService;

    public LearningContextFacade(AreaQueryService areaQueryService,
                                 EnrollmentQueryService enrollmentQueryService,
                                 CourseQueryService courseQueryService,
                                 ClassroomQueryService classroomQueryService) {
        this.areaQueryService = areaQueryService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.courseQueryService = courseQueryService;
        this.classroomQueryService = classroomQueryService;
    }

    public boolean isCoordinatorAssignedToAnyArea(Long coordinatorId) {
        if (coordinatorId == null) return false;
        return areaQueryService.handle(new ExistsAreaByCoordinatorIdQuery(coordinatorId));
    }

    public boolean isTeacherAssignedToAnyClassroom(Long teacherId) {
        if (teacherId == null) return false;
        return enrollmentQueryService.handle(new ExistsEnrollmentByUserIdAndRoleQuery(teacherId, "TEACHER"));
    }

    public boolean doesTopicBelongToCourse(Long topicId, Long courseId) {
        if (topicId == null || courseId == null) return false;
        return courseQueryService.handle(new ExistsTopicInCourseQuery(topicId, courseId));
    }

    public boolean doesCoordinatorOwnCourse(Long coordinatorId, Long courseId) {
        if (coordinatorId == null || courseId == null) return false;
        return courseQueryService.handle(new ExistsCourseForCoordinatorQuery(courseId, coordinatorId));
    }

    public Optional<UserEnrollmentData> getUserEnrollmentDataByCourse(Long userId, Long courseId) {
        if (userId == null || courseId == null) return Optional.empty();

        var classrooms = classroomQueryService.handle(new GetClassroomsByUserIdQuery(userId));

        return classrooms.stream()
                .filter(c -> c.getCourse().getId().equals(courseId))
                .findFirst()
                .map(c -> {
                    var section = c.getSection();

                    return new UserEnrollmentData(
                            section.getEducationLevel(),
                            section.getGradeLevel(),
                            courseId
                    );
                });
    }
}
