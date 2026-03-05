package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteAcademicLevelCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAcademicLevelByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAllAcademicLevelsQuery;
import com.hs.hstesis.learning.domain.services.AcademicLevelCommandService;
import com.hs.hstesis.learning.domain.services.AcademicLevelQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.*;
import com.hs.hstesis.learning.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/academic-levels", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Academic Levels", description = "Academic level management endpoints")
public class AcademicLevelController {
    private final AcademicLevelCommandService academicLevelCommandService;
    private final AcademicLevelQueryService academicLevelQueryService;

    public AcademicLevelController(AcademicLevelCommandService academicLevelCommandService, AcademicLevelQueryService academicLevelQueryService) {
        this.academicLevelCommandService = academicLevelCommandService;
        this.academicLevelQueryService = academicLevelQueryService;

    }

    @PostMapping("/create")
    public ResponseEntity<AcademicLevelResource> createAcademicLevel(@RequestBody CreateAcademicLevelResource createAcademicLevelResource) {
        var createAcademicLevelCommand = CreateAcademicLevelCommandFromResourceAssembler.toCommandFromResource(createAcademicLevelResource);
        var academicLevelId = academicLevelCommandService.handle(createAcademicLevelCommand);

        if (academicLevelId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getAcademicLevelByIdQuery = new GetAcademicLevelByIdQuery(academicLevelId);
        var academicLevel = academicLevelQueryService.handle(getAcademicLevelByIdQuery);

        if (academicLevel.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var academicLevelResource = AcademicLevelResourceFromEntityAssembler.toResourceFromEntity(academicLevel.get());
        return new ResponseEntity<>(academicLevelResource, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AcademicLevelResource>> getAllAcademicLevels() {
        var getAllAcademicLevelsQuery = new GetAllAcademicLevelsQuery();
        var academicLevels = academicLevelQueryService.handle(getAllAcademicLevelsQuery);
        var academicResources = academicLevels.stream()
                .map(AcademicLevelResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(academicResources);
    }

    @PutMapping("/update-name/{academicLevelId}")
    public ResponseEntity<AcademicLevelResource> updateAcademicLevelName(@PathVariable Long academicLevelId, @RequestBody UpdateAcademicLevelResource updateAcademicLevelResource) {

        var editAcademicLevelNameCommand = UpdateAcademicLevelCommandFromResourceAssembler.toCommandFromResource(academicLevelId, updateAcademicLevelResource);
        var updatedAcademicLevel = academicLevelCommandService.handle(editAcademicLevelNameCommand);

        if (updatedAcademicLevel.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var academicLevelResource = AcademicLevelResourceFromEntityAssembler.toResourceFromEntity(updatedAcademicLevel.get());
        return ResponseEntity.ok(academicLevelResource);
    }

    @DeleteMapping("/{academicLevelId}")
    public ResponseEntity<?> deleteAcademicLevel(@PathVariable Long academicLevelId) {
        var deleteAcademicLevelCommand = new DeleteAcademicLevelCommand(academicLevelId);
        academicLevelCommandService.handle(deleteAcademicLevelCommand);
        return ResponseEntity.ok("Academic level deleted successfully");
    }
}
