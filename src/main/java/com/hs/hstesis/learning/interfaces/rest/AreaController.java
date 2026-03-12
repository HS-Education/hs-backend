package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.iam.domain.model.queries.GetUserNameByIdQuery;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import com.hs.hstesis.learning.domain.model.commands.DeleteAreaCommand;
import com.hs.hstesis.learning.domain.model.queries.GetAllAreasQuery;
import com.hs.hstesis.learning.domain.model.queries.GetAreaByIdQuery;
import com.hs.hstesis.learning.domain.services.AreaCommandService;
import com.hs.hstesis.learning.domain.services.AreaQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaResource;
import com.hs.hstesis.learning.interfaces.rest.resources.CreateAreaResource;
import com.hs.hstesis.learning.interfaces.rest.resources.UpdateAreaResource;
import com.hs.hstesis.learning.interfaces.rest.transform.AreaResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.CreateAreaCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.UpdateAreaCommandFromResourceAssembler;
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
@RequestMapping(value = "/api/v1/areas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Areas", description = "Area management endpoints")
public class AreaController {
    private final AreaCommandService areaCommandService;
    private final AreaQueryService areaQueryService;
    private final UserQueryService userQueryService;

    public AreaController(AreaCommandService areaCommandService,
                          AreaQueryService areaQueryService,
                          UserQueryService userQueryService) {
        this.areaCommandService = areaCommandService;
        this.areaQueryService = areaQueryService;
        this.userQueryService = userQueryService;
    }

    @Operation(description = "Creates a new area.")
    @PostMapping
    public ResponseEntity<AreaResource> createArea(@RequestBody CreateAreaResource createAreaResource) {

        var createAreaCommand = CreateAreaCommandFromResourceAssembler.toCommandFromResource(createAreaResource);
        var areaId = areaCommandService.handle(createAreaCommand);

        if(areaId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getAreaByIdQuery = new GetAreaByIdQuery(areaId);
        var area = areaQueryService.handle(getAreaByIdQuery);

        if(area.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var getUsernameByIdQuery = new GetUserNameByIdQuery(createAreaResource.coordinatorId());
        var coordinatorUsername = userQueryService.handle(getUsernameByIdQuery);

        if(coordinatorUsername.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var areaResource = AreaResourceFromEntityAssembler.toResourceFromEntity(area.get(), coordinatorUsername.get());
        return new ResponseEntity<>(areaResource, HttpStatus.CREATED);
    }

    @Operation(description = "Returns a list of all areas.")
    @GetMapping
    public ResponseEntity<List<AreaResource>> getAllAreas() {
        var getAllAreasQuery = new GetAllAreasQuery();
        var areas = areaQueryService.handle(getAllAreasQuery);

        var areaResources = areas.stream()
                .map(area -> {
                    var getUsernameByIdQuery = new GetUserNameByIdQuery(area.getCoordinatorId());
                    var coordinatorUsername = userQueryService.handle(getUsernameByIdQuery)
                            .orElse("Unknown");
                    return AreaResourceFromEntityAssembler.toResourceFromEntity(area, coordinatorUsername);
                })
                .toList();
        return ResponseEntity.ok(areaResources);
    }

    @Operation(description = "Updates the details of an existing area.")
    @PatchMapping("/{areaId}")
    public ResponseEntity<AreaResource> updateArea(@PathVariable Long areaId, @RequestBody UpdateAreaResource updateAreaResource) {

        var updateAreaCommand = UpdateAreaCommandFromResourceAssembler.toCommandFromResource(areaId, updateAreaResource);
        var updatedArea = areaCommandService.handle(updateAreaCommand);

        if(updatedArea.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var getUsernameByIdQuery = new GetUserNameByIdQuery(updatedArea.get().getCoordinatorId());
        var coordinatorUsername = userQueryService.handle(getUsernameByIdQuery);

        if(coordinatorUsername.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var areaResource = AreaResourceFromEntityAssembler.toResourceFromEntity(updatedArea.get(), coordinatorUsername.get());
        return ResponseEntity.ok(areaResource);
    }

    @Operation(description = "Deletes an existing area. It must not have related courses.")
    @DeleteMapping("/{areaId}")
    public ResponseEntity<MessageResource> deleteArea(@PathVariable Long areaId) {

        var deleteAreaCommand = new DeleteAreaCommand(areaId);
        areaCommandService.handle(deleteAreaCommand);
        return ResponseEntity.ok(new MessageResource("Area deleted successfully"));
    }
}
