package com.hs.hstesis.onboarding.interfaces.rest;

import com.hs.hstesis.onboarding.domain.model.commands.CreatePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.commands.DeletePlatformTutorialCommand;
import com.hs.hstesis.onboarding.domain.model.queries.GetAllPlatformTutorialsQuery;
import com.hs.hstesis.onboarding.domain.services.PlatformTutorialCommandService;
import com.hs.hstesis.onboarding.domain.services.PlatformTutorialQueryService;
import com.hs.hstesis.onboarding.interfaces.rest.resources.CreatePlatformTutorialResource;
import com.hs.hstesis.onboarding.interfaces.rest.resources.PlatformTutorialResource;
import com.hs.hstesis.onboarding.interfaces.rest.transform.PlatformTutorialResourceFromEntityAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/onboarding/tutorials", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tutorials", description = "Platform Tutorials Endpoints")
public class PlatformTutorialController {

    private final PlatformTutorialCommandService commandService;
    private final PlatformTutorialQueryService queryService;

    public PlatformTutorialController(PlatformTutorialCommandService commandService, PlatformTutorialQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Get all platform tutorials")
    @GetMapping
    public ResponseEntity<List<PlatformTutorialResource>> getAllTutorials() {
        var tutorials = queryService.handle(new GetAllPlatformTutorialsQuery());
        var resources = tutorials.stream()
                .map(PlatformTutorialResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Create a platform tutorial")
    @PostMapping
    public ResponseEntity<PlatformTutorialResource> createTutorial(@RequestBody CreatePlatformTutorialResource resource) {
        var command = new CreatePlatformTutorialCommand(resource.title(), resource.description(), resource.fileUrl());
        var id = commandService.handle(command);
        
        if (id.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Return dummy resource for now or refactor to fetch it properly
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Delete a platform tutorial")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResource> deleteTutorial(@PathVariable Long id) {
        commandService.handle(new DeletePlatformTutorialCommand(id));
        return ResponseEntity.ok(new MessageResource("Tutorial deleted successfully"));
    }
}

