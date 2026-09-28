package com.hs.hstesis.achievements.interfaces.rest;

import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateStudentInsightCommand;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.interfaces.rest.resources.StudentAchievementResource;
import com.hs.hstesis.achievements.interfaces.rest.resources.AreaAchievementResource;
import com.hs.hstesis.achievements.interfaces.rest.resources.ClassroomAchievementResource;
import com.hs.hstesis.achievements.interfaces.rest.transform.AreaAchievementResourceFromEntityAssembler;
import com.hs.hstesis.achievements.interfaces.rest.transform.ClassroomAchievementResourceFromEntityAssembler;
import com.hs.hstesis.achievements.interfaces.rest.transform.StudentAchievementResourceFromEntityAssembler;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1/achievements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Achievements", description = "Endpoints for academic performance and AI insights")
public class AchievementController {

    private final AchievementQueryService achievementQueryService;
    private final AchievementCommandService achievementCommandService;
    private final AchievementInsightRepository achievementInsightRepository;
    private final com.hs.hstesis.learning.interfaces.acl.LearningContextFacade learningContextFacade;
    private final com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade assessmentsContextFacade;

    public AchievementController(AchievementQueryService achievementQueryService,
                                 AchievementCommandService achievementCommandService,
                                 AchievementInsightRepository achievementInsightRepository,
                                 com.hs.hstesis.learning.interfaces.acl.LearningContextFacade learningContextFacade,
                                 com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade assessmentsContextFacade) {
        this.achievementQueryService = achievementQueryService;
        this.achievementCommandService = achievementCommandService;
        this.achievementInsightRepository = achievementInsightRepository;
        this.learningContextFacade = learningContextFacade;
        this.assessmentsContextFacade = assessmentsContextFacade;
    }

    @Operation(summary = "Get student performance and latest AI insight")
    @GetMapping("/students/{studentId}")
    public ResponseEntity<StudentAchievementResource> getStudentAchievements(@PathVariable Long studentId) {
        Optional<StudentPerformance> performanceOpt = achievementQueryService.handle(new GetStudentPerformanceQuery(studentId));
        if (performanceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var latestInsightOpt = achievementInsightRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc("STUDENT", studentId);
        String insightText = latestInsightOpt.map(i -> i.getInsightText()).orElse(null);

        return ResponseEntity.ok(StudentAchievementResourceFromEntityAssembler.toResourceFromEntity(performanceOpt.get(), insightText));
    }

    @Operation(summary = "Get student performance summary for a specific grading period")
    @GetMapping("/students/{studentId}/courses/{courseId}/periods/{gradingPeriodId}/summary")
    public ResponseEntity<com.hs.hstesis.achievements.interfaces.rest.resources.StudentPerformanceSummaryResource> getStudentPerformanceSummary(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            @PathVariable Long gradingPeriodId) {
        
        var query = new com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceSummaryQuery(studentId, courseId, gradingPeriodId);
        var summaryOpt = achievementQueryService.handle(query);
        
        if (summaryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var summary = summaryOpt.get();
        var resource = new com.hs.hstesis.achievements.interfaces.rest.resources.StudentPerformanceSummaryResource(
                summary.studentId(),
                summary.gradingPeriodId(),
                summary.bimesterAverage(),
                summary.weeklyProgression().stream().map(w -> new com.hs.hstesis.achievements.interfaces.rest.resources.StudentPerformanceSummaryResource.WeeklyPerformanceResource(
                        w.weekNumber(), w.averageScore(), w.needsRemedial()
                )).toList(),
                summary.definitiveGrades().stream().map(d -> new com.hs.hstesis.achievements.interfaces.rest.resources.StudentPerformanceSummaryResource.DefinitiveGradeResource(
                        d.questionnaireId(), d.weekNumber(), d.score()
                )).toList()
        );

        return ResponseEntity.ok(resource);
    }

    @Operation(summary = "Generate and save a new AI insight for a student")
    @PostMapping("/students/{studentId}/insights")
    public ResponseEntity<?> generateStudentInsight(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "Estudiante") String studentName) {
        try {
            var insight = achievementCommandService.handle(new GenerateStudentInsightCommand(studentId, studentName));
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Insight generado correctamente",
                    "insightText", insight.getInsightText()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get classroom performance")
    @GetMapping("/classrooms/{classroomId}")
    public ResponseEntity<ClassroomAchievementResource> getClassroomAchievements(@PathVariable Long classroomId) {
        Optional<ClassroomPerformance> performanceOpt = achievementQueryService.handle(new GetClassroomPerformanceQuery(classroomId));
        if (performanceOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var latestInsightOpt = achievementInsightRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc("CLASSROOM", classroomId);
        String insightText = latestInsightOpt.map(i -> i.getInsightText()).orElse(null);
        java.util.Date insightCreatedAt = latestInsightOpt.map(i -> i.getCreatedAt()).orElse(null);

        return ResponseEntity.ok(new ClassroomAchievementResource(performanceOpt.get(), insightText, insightCreatedAt));
    }

    @Operation(summary = "Get classroom questionnaire coverage and question-level progress")
    @GetMapping("/classrooms/{classroomId}/progress-details")
    @PreAuthorize("hasAnyRole('TEACHER', 'COORDINATOR')")
    public ResponseEntity<java.util.List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireProgressDto>>
    getClassroomProgressDetails(@PathVariable Long classroomId,
            org.springframework.security.core.Authentication authentication) {
        var userDetails = (com.hs.hstesis.iam.infrastructure.authorization.sfs.model.UserDetailsImpl)
                authentication.getPrincipal();
        if (!learningContextFacade.canViewClassroomProgress(userDetails.getId(), classroomId)) {
            throw new org.springframework.security.access.AccessDeniedException("Classroom progress is not available.");
        }
        var courseId = learningContextFacade.getCourseIdByClassroomId(classroomId)
                .orElseThrow(() -> new com.hs.hstesis.shared.domain.exceptions.ResourceNotFoundException(
                        "Classroom not found."));
        var studentIds = learningContextFacade.getStudentsByClassroom(classroomId).stream()
                .map(com.hs.hstesis.learning.interfaces.acl.dto.ClassroomStudentData::userId)
                .collect(java.util.stream.Collectors.toSet());
        return ResponseEntity.ok(assessmentsContextFacade.getCourseProgress(courseId, studentIds));
    }

    @Operation(summary = "Generate insight for classroom")
    @PostMapping("/classrooms/{classroomId}/insights")
    @PreAuthorize("hasRole('TEACHER') and !hasRole('COORDINATOR')")
    public ResponseEntity<?> generateClassroomInsight(@PathVariable Long classroomId) {
        try {
            var insight = achievementCommandService.handle(new GenerateClassroomInsightCommand(classroomId));
            return ResponseEntity.ok(java.util.Map.of("insightText", insight.getInsightText()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get area performance")
    @GetMapping("/areas/{areaId}")
    public ResponseEntity<AreaAchievementResource> getAreaAchievements(@PathVariable Long areaId) {
        Optional<com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance> performanceOpt = achievementQueryService.handle(new com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery(areaId));
        if (performanceOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        var latestInsightOpt = achievementInsightRepository.findTopByEntityTypeAndEntityIdOrderByCreatedAtDesc("AREA", areaId);
        String insightText = latestInsightOpt.map(i -> i.getInsightText()).orElse(null);
        java.util.Date insightCreatedAt = latestInsightOpt.map(i -> i.getCreatedAt()).orElse(null);

        return ResponseEntity.ok(new AreaAchievementResource(performanceOpt.get(), insightText, insightCreatedAt));
    }

    @Operation(summary = "Generate insight for area")
    @PostMapping("/areas/{areaId}/insights")
    public ResponseEntity<?> generateAreaInsight(@PathVariable Long areaId) {
        try {
            var insight = achievementCommandService.handle(new com.hs.hstesis.achievements.domain.model.commands.GenerateAreaInsightCommand(areaId));
            return ResponseEntity.ok(java.util.Map.of("insightText", insight.getInsightText()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate complementary recommendations (quiz) for a classroom based on AI")
    @PostMapping("/classrooms/{classroomId}/recommendations")
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<?> generateClassroomRecommendation(
            @PathVariable Long classroomId, 
            @RequestBody com.hs.hstesis.achievements.interfaces.rest.resources.GenerateRecommendationResource resource) {
        try {
            var command = new com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomRecommendationCommand(
                    classroomId, resource.topicName(), resource.contextText(), resource.numQuestions());
            var response = achievementCommandService.handle(command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
