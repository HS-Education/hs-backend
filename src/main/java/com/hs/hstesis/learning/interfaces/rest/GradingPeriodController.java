package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteGradingPeriodCommand;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodsByAcademicYearIdQuery;
import com.hs.hstesis.learning.domain.services.GradingPeriodCommandService;
import com.hs.hstesis.learning.domain.services.GradingPeriodQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateGradingPeriodResource;
import com.hs.hstesis.learning.interfaces.rest.resources.GradingPeriodResource;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateGradingPeriodCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.GradingPeriodResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/grading-periods", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Grading Periods", description = "Grading period management endpoints")
public class GradingPeriodController {
    private final GradingPeriodCommandService gradingPeriodCommandService;
    private final GradingPeriodQueryService gradingPeriodQueryService;

    public GradingPeriodController(GradingPeriodCommandService gradingPeriodCommandService, GradingPeriodQueryService gradingPeriodQueryService) {
        this.gradingPeriodCommandService = gradingPeriodCommandService;
        this.gradingPeriodQueryService = gradingPeriodQueryService;
    }

    @PostMapping("/create")
    public ResponseEntity<GradingPeriodResource> createGradingPeriod(CreateGradingPeriodResource createGradingPeriodResource) {
        var createGradingPeriodCommand = CreateGradingPeriodCommandFromResourceAssembler.toCommandFromResource(createGradingPeriodResource);
        var gradingPeriodId = gradingPeriodCommandService.handle(createGradingPeriodCommand);

        if(gradingPeriodId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getGradingPeriodByIdQuery = new GetGradingPeriodByIdQuery(gradingPeriodId);
        var gradingPeriod = gradingPeriodQueryService.handle(getGradingPeriodByIdQuery);

        if(gradingPeriod.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var gradingPeriodResource = GradingPeriodResourceFromEntityAssembler.toResourceFromEntity(gradingPeriod.get());
        return new ResponseEntity<>(gradingPeriodResource, HttpStatus.CREATED);
    }

    @GetMapping("/{academicYearId}")
    public ResponseEntity<List<GradingPeriodResource>> getGradingPeriodsByAcademicYearId(@PathVariable Long academicYearId) {
        var getGradingPeriodsByAcademicYearId = new GetGradingPeriodsByAcademicYearIdQuery(academicYearId);
        var gradingPeriods = gradingPeriodQueryService.handle(getGradingPeriodsByAcademicYearId);
        var gradingPeriodResources = gradingPeriods.stream()
                .map(GradingPeriodResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(gradingPeriodResources);
    }

    @DeleteMapping("/{gradingPeriodId}")
    public ResponseEntity<?> deleteGradingPeriod(@PathVariable Long gradingPeriodId) {
        var deleteGradingPeriodCommand = new DeleteGradingPeriodCommand(gradingPeriodId);
        gradingPeriodCommandService.handle(deleteGradingPeriodCommand);
        return ResponseEntity.ok("Grading period deleted successfully");
    }

}
