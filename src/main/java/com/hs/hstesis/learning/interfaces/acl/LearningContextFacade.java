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
    private final com.hs.hstesis.learning.domain.services.GradingPeriodQueryService gradingPeriodQueryService;

    public LearningContextFacade(AreaQueryService areaQueryService,
                                 EnrollmentQueryService enrollmentQueryService,
                                 CourseQueryService courseQueryService,
                                 ClassroomQueryService classroomQueryService,
                                 com.hs.hstesis.learning.domain.services.GradingPeriodQueryService gradingPeriodQueryService) {
        this.areaQueryService = areaQueryService;
        this.enrollmentQueryService = enrollmentQueryService;
        this.courseQueryService = courseQueryService;
        this.classroomQueryService = classroomQueryService;
        this.gradingPeriodQueryService = gradingPeriodQueryService;
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

    public boolean existsCourse(Long courseId) {
        if (courseId == null) return false;
        return courseQueryService.handle(new GetCourseByIdQuery(courseId)).isPresent();
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
    public List<Long> getEnrolledCourseIds(Long userId) {
        if (userId == null) return List.of();
        
        var classrooms = classroomQueryService.handle(new GetClassroomsByUserIdQuery(userId));
        return classrooms.stream()
                .map(c -> c.getCourse().getId())
                .distinct()
                .toList();
    }

    public Optional<com.hs.hstesis.learning.interfaces.acl.dto.GradingPeriodData> getGradingPeriodByCourseAndBimester(Long courseId, String bimester) {
        if (courseId == null || bimester == null) return Optional.empty();

        var course = courseQueryService.handle(new GetCourseByIdQuery(courseId));
        if (course.isEmpty()) return Optional.empty();

        var classrooms = classroomQueryService.handle(new GetClassroomsByCourseIdQuery(courseId));
        if (classrooms.isEmpty()) return Optional.empty();
        
        var academicYearId = classrooms.get(0).getAcademicYear().getId();
        
        var gradingPeriods = gradingPeriodQueryService.handle(new com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodsByAcademicYearIdQuery(academicYearId));
        
        return gradingPeriods.stream()
                .filter(gp -> gp.getBimester().name().equals(bimester))
                .findFirst()
                .map(gp -> new com.hs.hstesis.learning.interfaces.acl.dto.GradingPeriodData(
                        gp.getId(),
                        gp.getBimester().name(),
                        gp.getStartDate(),
                        gp.getEndDate()
                ));
    }

    public Optional<com.hs.hstesis.learning.domain.model.entities.Topic> getTopicByCourseAndGradingPeriodAndOrderIndex(Long courseId, Long gradingPeriodId, Integer orderIndex) {
        if (courseId == null || gradingPeriodId == null || orderIndex == null) return Optional.empty();
        var course = courseQueryService.handle(new GetCourseByIdQuery(courseId));
        if (course.isEmpty()) return Optional.empty();
        
        return course.get().getTopics().stream()
                .filter(t -> t.getGradingPeriod().getId().equals(gradingPeriodId) && t.getOrderIndex().equals(orderIndex))
                .findFirst();
    }
}
