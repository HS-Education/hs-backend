package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.GenerateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicYearByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicYearsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetGradingPeriodsByAcademicYearIdQuery;
import com.hs.hstesis.learning.domain.services.AcademicYearCommandService;
import com.hs.hstesis.learning.domain.services.AcademicYearQueryService;
import com.hs.hstesis.learning.domain.services.GradingPeriodCommandService;
import com.hs.hstesis.learning.domain.services.GradingPeriodQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.AcademicYearResource;
import com.hs.hstesis.learning.interfaces.rest.resources.GradingPeriodResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateGradingPeriodResource;
import com.hs.hstesis.learning.interfaces.rest.transform.AcademicYearResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.GradingPeriodResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateGradingPeriodCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/academic-years", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Academic Years", description = "Academic year management endpoints")
public class AcademicYearController {
    private final AcademicYearCommandService academicYearCommandService;
    private final AcademicYearQueryService academicYearQueryService;
    private final GradingPeriodQueryService gradingPeriodQueryService;
    private final GradingPeriodCommandService gradingPeriodCommandService;

    public AcademicYearController(
            AcademicYearCommandService academicYearCommandService,
            AcademicYearQueryService academicYearQueryService,
            GradingPeriodQueryService gradingPeriodQueryService,
            GradingPeriodCommandService gradingPeriodCommandService) {

        this.academicYearCommandService = academicYearCommandService;
        this.academicYearQueryService = academicYearQueryService;
        this.gradingPeriodQueryService = gradingPeriodQueryService;
        this.gradingPeriodCommandService = gradingPeriodCommandService;
    }

@PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Create a new academic year.")
    @PostMapping
    public ResponseEntity<AcademicYearResource> generateYear() {
        var academicYearId = academicYearCommandService.handle(new GenerateAcademicYearCommand());

        if (academicYearId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getAcademicYearByIdQuery = new GetAcademicYearByIdQuery(academicYearId);
        var academicYear = academicYearQueryService.handle(getAcademicYearByIdQuery);

        if (academicYear.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var academicYearResource = AcademicYearResourceFromEntityAssembler.toResourceFromEntity(academicYear.get());
        return new ResponseEntity<>(academicYearResource, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Return a list of all academic years.")
    @GetMapping
    public ResponseEntity<List<AcademicYearResource>> getAllAcademicYears() {
        var getAllAcademicYearsQuery = new GetAllAcademicYearsQuery();
        var academicYears = academicYearQueryService.handle(getAllAcademicYearsQuery);

        if (academicYears.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var resources = academicYears.stream()
                .map(AcademicYearResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasRole('ADMIN') || hasRole('COORDINATOR') || hasRole('TEACHER') || hasRole('STUDENT')")
    @Operation(description = "Return grading periods of an academic year.")
    @GetMapping("/{academicYearId}/grading-periods")
    public ResponseEntity<List<GradingPeriodResource>> getGradingPeriods(@PathVariable Long academicYearId) {

        var getGradingPeriodsByAcademicYearIdQuery = new GetGradingPeriodsByAcademicYearIdQuery(academicYearId);
        var periods = gradingPeriodQueryService.handle(getGradingPeriodsByAcademicYearIdQuery);

        if (periods.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        var resources = periods.stream()
                .map(GradingPeriodResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Configure dates of a grading period.")
    @PutMapping("/{academicYearId}/grading-periods/{gradingPeriodId}")
    public ResponseEntity<GradingPeriodResource> updateGradingPeriod(
            @PathVariable Long academicYearId,
            @PathVariable Long gradingPeriodId,
            @RequestBody UpdateGradingPeriodResource resource) {

        var updateGradingPeriodCommand = UpdateGradingPeriodCommandFromResourceAssembler.toCommandFromResource(academicYearId, gradingPeriodId, resource);
        var updatedGradingPeriod = gradingPeriodCommandService.handle(updateGradingPeriodCommand);

        if (updatedGradingPeriod.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var getGradingPeriodByIdQuery = new GetGradingPeriodByIdQuery(updatedGradingPeriod.get().getId());
        var gradingPeriod = gradingPeriodQueryService.handle(getGradingPeriodByIdQuery);

        if (gradingPeriod.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var gradingPeriodResource = GradingPeriodResourceFromEntityAssembler.toResourceFromEntity(gradingPeriod.get());

        return ResponseEntity.ok(gradingPeriodResource);
    }
}
