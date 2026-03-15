package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllSectionsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionsByEducationAndGradeLevelQuery;
import com.hs.hstesis.learning.domain.model.valueobjects.EducationLevel;
import com.hs.hstesis.learning.domain.model.valueobjects.GradeLevel;
import com.hs.hstesis.learning.domain.services.SectionCommandService;
import com.hs.hstesis.learning.domain.services.SectionQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateSectionResource;
import com.hs.hstesis.learning.interfaces.rest.resources.SectionResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateSectionResource;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateSectionCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.SectionResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateSectionCommandFromResourceAssembler;
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
@RequestMapping(value = "/api/v1/sections", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Sections", description = "Section management endpoints")
public class SectionController {
    private final SectionCommandService sectionCommandService;
    private final SectionQueryService sectionQueryService;

    public SectionController(SectionCommandService sectionCommandService, SectionQueryService sectionQueryService) {
        this.sectionCommandService = sectionCommandService;
        this.sectionQueryService = sectionQueryService;
    }

    @Operation(description = "Create a new section.")
    @PostMapping()
    public ResponseEntity<SectionResource> createSection(@RequestBody CreateSectionResource createSectionResource) {
        var createSectionCommand = CreateSectionCommandFromResourceAssembler.toCommandFromResource(createSectionResource);
        var sectionId = sectionCommandService.handle(createSectionCommand);

        if (sectionId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getSectionByIdQuery = new GetSectionByIdQuery(sectionId);
        var section = sectionQueryService.handle(getSectionByIdQuery);

        if (section.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var sectionResource = SectionResourceFromEntityAssembler.toResourceFromEntity(section.get());
        return new ResponseEntity<>(sectionResource, HttpStatus.CREATED);
    }

    @Operation(description = "Returns a list of all sections, optionally filtered by education and grade level.")
    @GetMapping
    public ResponseEntity<List<SectionResource>> getSections(
            @RequestParam(required = false) EducationLevel educationLevel,
            @RequestParam(required = false) GradeLevel gradeLevel
    ) {
        var sections = (educationLevel != null && gradeLevel != null)
                ? sectionQueryService.handle(new GetSectionsByEducationAndGradeLevelQuery(educationLevel, gradeLevel))
                : sectionQueryService.handle(new GetAllSectionsQuery());

        if (sections.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var resources = sections.stream()
                .map(SectionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }

    @Operation(description = "Update the details of an existing section.")
    @PatchMapping("/{sectionId}")
    public ResponseEntity<SectionResource> updateSection(@PathVariable Long sectionId, @RequestBody UpdateSectionResource updateSectionResource) {
        var updateSectionCommand = UpdateSectionCommandFromResourceAssembler.toCommandFromResource(sectionId, updateSectionResource);
        var updatedSection = sectionCommandService.handle(updateSectionCommand);

        if (updatedSection.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var sectionResource = SectionResourceFromEntityAssembler.toResourceFromEntity(updatedSection.get());
        return ResponseEntity.ok(sectionResource);
    }

    @Operation(description = "Delete an existing section.")
    @DeleteMapping("/{sectionId}")
    public ResponseEntity<MessageResource> deleteSection(@PathVariable Long sectionId) {
        var deleteSectionCommand = new DeleteSectionCommand(sectionId);
        sectionCommandService.handle(deleteSectionCommand);
        return ResponseEntity.ok(new MessageResource("Section deleted successfully."));
    }
}
