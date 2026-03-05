package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.AddCourseToStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.commands.RemoveCourseFromStudyPlanCommand;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdAndCourseIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetStudyPlanByAcademicLevelIdQuery;
import com.hs.hstesis.learning.domain.services.StudyPlanCommandService;
import com.hs.hstesis.learning.domain.services.StudyPlanQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.StudyPlanResource;
import com.hs.hstesis.learning.interfaces.rest.transform.StudyPlanResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/academic-levels/{academicLevelId}/courses", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Study Plan", description = "Endpoints for managing the study plan of an academic level")
public class StudyPlanController {
    private final StudyPlanCommandService studyPlanCommandService;
    private final StudyPlanQueryService studyPlanQueryService;

    public StudyPlanController(StudyPlanCommandService studyPlanCommandService, StudyPlanQueryService studyPlanQueryService) {
        this.studyPlanCommandService = studyPlanCommandService;
        this.studyPlanQueryService = studyPlanQueryService;
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<StudyPlanResource> addCourseToAcademicLevel(
            @PathVariable Long academicLevelId,
            @PathVariable Long courseId) {

        var addCourseToStudyPlanCommand = new AddCourseToStudyPlanCommand(academicLevelId, courseId);
        var studyPlanId = studyPlanCommandService.handle(addCourseToStudyPlanCommand);

        if (studyPlanId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getStudyPlanByAcademicLevelIdAndCourseIdQuery = new GetStudyPlanByAcademicLevelIdAndCourseIdQuery(academicLevelId, courseId);
        var studyPlan = studyPlanQueryService.handle(getStudyPlanByAcademicLevelIdAndCourseIdQuery);

        if(studyPlan.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var studyPlanResource = StudyPlanResourceFromEntityAssembler.toResourceFromEntity(studyPlan.get());
        return new ResponseEntity<>(studyPlanResource, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StudyPlanResource>> getCoursesByAcademicLevel(@PathVariable Long academicLevelId) {

        var getStudyPlanByAcademicLevelIdQuery = new GetStudyPlanByAcademicLevelIdQuery(academicLevelId);
        var studyPlans = studyPlanQueryService.handle(getStudyPlanByAcademicLevelIdQuery);

        var studyPlanResources = studyPlans.stream()
                .map(StudyPlanResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(studyPlanResources);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> removeCourseFromAcademicLevel(
            @PathVariable Long academicLevelId,
            @PathVariable Long courseId) {

        var removeCourseFromStudyPlanCommand = new RemoveCourseFromStudyPlanCommand(academicLevelId, courseId);
        studyPlanCommandService.handle(removeCourseFromStudyPlanCommand);

        return ResponseEntity.ok("Course removed from academic level successfully.");
    }
}
