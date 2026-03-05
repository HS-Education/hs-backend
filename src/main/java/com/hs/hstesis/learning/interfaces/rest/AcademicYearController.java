package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.ActivateAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.CloseAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicYearCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicYearByIdQuery;
import com.hs.hstesis.learning.domain.services.AcademicYearCommandService;
import com.hs.hstesis.learning.domain.services.AcademicYearQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.AcademicYearResource;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateAcademicYearResource;
import com.hs.hstesis.learning.interfaces.rest.transform.AcademicYearResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateAcademicYearCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/academic-years", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Academic Years", description = "Academic year management endpoints")
public class AcademicYearController {
    private final AcademicYearCommandService academicYearCommandService;
    private final AcademicYearQueryService academicYearQueryService;

    public AcademicYearController(AcademicYearCommandService academicYearCommandService, AcademicYearQueryService academicYearQueryService) {
        this.academicYearCommandService = academicYearCommandService;
        this.academicYearQueryService = academicYearQueryService;
    }

    @PostMapping("/create")
    public ResponseEntity<AcademicYearResource> createAcademicYear(@RequestBody CreateAcademicYearResource createAcademicYearResource) {
        var createAcademicYearCommand = CreateAcademicYearCommandFromResourceAssembler.toCommandFromResource(createAcademicYearResource);
        var academicYearId = academicYearCommandService.handle(createAcademicYearCommand);

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

    @PutMapping("/activate-year/{academicYearId}")
    public ResponseEntity<Void> activateAcademicYear(@PathVariable Long academicYearId) {
        var activateAcademicYearCommand = new ActivateAcademicYearCommand(academicYearId);
        academicYearCommandService.handle(activateAcademicYearCommand);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/close-year/{academicYearId}")
    public ResponseEntity<Void> closeAcademicYear(@PathVariable Long academicYearId) {
        var closeAcademicYearCommand = new CloseAcademicYearCommand(academicYearId);
        academicYearCommandService.handle(closeAcademicYearCommand);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{academicYearId}")
    public ResponseEntity<?> deleteAcademicYear(@PathVariable Long academicYearId) {
        var deleteAcademicYearCommand = new DeleteAcademicYearCommand(academicYearId);
        academicYearCommandService.handle(deleteAcademicYearCommand);
        return ResponseEntity.ok("Academic year deleted successfully");
    }
}
