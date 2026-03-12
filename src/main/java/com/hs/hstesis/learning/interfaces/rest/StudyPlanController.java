package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.commands.RemoveCourseFromStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByEducationLevelAndGradeLevelAndCourseIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByEducationLevelAndGradeLevelQuery;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.learning.domain.services.StudyPlanCommandService;
import com.hs.hstesis.learning.domain.services.StudyPlanQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.StudyPlanResource;
import com.hs.hstesis.learning.interfaces.rest.transform.StudyPlanResourceFromEntityAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping(value = "/api/v1/study-plan/courses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Study Plan", description = "Endpoints for managing the study plan of an academic level")
public class StudyPlanController {
    private final StudyPlanCommandService studyPlanCommandService;
    private final StudyPlanQueryService studyPlanQueryService;

    public StudyPlanController(StudyPlanCommandService studyPlanCommandService, StudyPlanQueryService studyPlanQueryService) {
        this.studyPlanCommandService = studyPlanCommandService;
        this.studyPlanQueryService = studyPlanQueryService;
    }

    @Operation(description = "Add a course to the study plan of an academic level.")
    @PostMapping("/{courseId}")
    public ResponseEntity<StudyPlanResource> addCourseToAcademicLevel(
            @PathVariable Long courseId,
            @RequestParam EducationLevel educationLevel,
            @RequestParam GradeLevel gradeLevel) {

        var addCourseToStudyPlanCommand = new AddCourseToStudyPlanCommand(educationLevel, gradeLevel, courseId);
        var studyPlanId = studyPlanCommandService.handle(addCourseToStudyPlanCommand);

        if (studyPlanId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getStudyPlanByAcademicLevelIdAndCourseIdQuery = new GetStudyPlanByEducationLevelAndGradeLevelAndCourseIdQuery(educationLevel, gradeLevel, courseId);
        var studyPlan = studyPlanQueryService.handle(getStudyPlanByAcademicLevelIdAndCourseIdQuery);

        if(studyPlan.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var studyPlanResource = StudyPlanResourceFromEntityAssembler.toResourceFromEntity(studyPlan.get());
        return new ResponseEntity<>(studyPlanResource, HttpStatus.CREATED);
    }

    @Operation(description = "Get the study plan of an academic level, including all courses.")
    @GetMapping
    public ResponseEntity<List<StudyPlanResource>> getCoursesByAcademicLevel(
            @RequestParam EducationLevel educationLevel,
            @RequestParam GradeLevel gradeLevel) {

        var getStudyPlanByAcademicLevelIdQuery = new GetStudyPlanByEducationLevelAndGradeLevelQuery(educationLevel, gradeLevel);
        var studyPlans = studyPlanQueryService.handle(getStudyPlanByAcademicLevelIdQuery);

        if (studyPlans.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var studyPlanResources = studyPlans.stream()
                .map(StudyPlanResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(studyPlanResources);
    }

    @Operation(description = "Remove a course from the study plan of an academic level.")
    @DeleteMapping("/{studyPlanId}")
    public ResponseEntity<MessageResource> removeCourseFromAcademicLevel(@PathVariable Long studyPlanId) {

        studyPlanCommandService.handle(new RemoveCourseFromStudyPlanCommand(studyPlanId));

        return ResponseEntity.ok(new MessageResource("Course removed from academic level successfully."));
    }
}
