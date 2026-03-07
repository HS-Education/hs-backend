package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.commands.DeleteSectionCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllSectionsQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionByIdQuery;
import com.hs.hstesis.learning.domain.model.queries.GetSectionsByAcademicLevelIdQuery;
import com.hs.hstesis.learning.domain.services.SectionCommandService;
import com.hs.hstesis.learning.domain.services.SectionQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateSectionResource;
import com.hs.hstesis.learning.interfaces.rest.resources.SectionResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateSectionResource;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateSectionCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.SectionResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateSectionCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/create")
    public ResponseEntity<SectionResource> createSection(@RequestBody CreateSectionResource createSectionResource) {
        var createSectionCommand = CreateSectionCommandFromResourceAssembler.toCommandFromResource(createSectionResource);
        var sectionId = sectionCommandService.handle(createSectionCommand);

        if (sectionId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getSectionByIdQuery = new GetSectionByIdQuery(sectionId);
        var section = sectionQueryService.handle(getSectionByIdQuery);

        if (section.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var sectionResource = SectionResourceFromEntityAssembler.toResourceFromEntity(section.get());
        return new ResponseEntity<>(sectionResource, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SectionResource>> getAllSections() {
        var getAllSectionsQuery = new GetAllSectionsQuery();
        var sections = sectionQueryService.handle(getAllSectionsQuery);
        var sectionResources = sections.stream()
                .map(SectionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(sectionResources);
    }

    @GetMapping("/{academicLevelId}")
    public ResponseEntity<List<SectionResource>> getSectionsByAcademicLevelId(@PathVariable Long academicLevelId) {
        var getSectionsByAcademicLevelIdQuery = new GetSectionsByAcademicLevelIdQuery(academicLevelId);
        var sections = sectionQueryService.handle(getSectionsByAcademicLevelIdQuery);
        var sectionResources = sections.stream()
                .map(SectionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(sectionResources);
    }

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

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<?> deleteSection(@PathVariable Long sectionId) {
        var deleteSectionCommand = new DeleteSectionCommand(sectionId);
        sectionCommandService.handle(deleteSectionCommand);
        return ResponseEntity.ok("Section deleted successfully");
    }
}
