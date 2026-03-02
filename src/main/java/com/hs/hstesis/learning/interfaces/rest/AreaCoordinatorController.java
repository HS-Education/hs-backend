package com.hs.hstesis.learning.interfaces.rest;

import com.hs.hstesis.learning.domain.model.queries.GetAreaCoordinatorByIdQuery;
import com.hs.hstesis.learning.domain.services.AreaCoordinatorCommandService;
import com.hs.hstesis.learning.domain.services.AreaCoordinatorQueryService;
import com.hs.hstesis.learning.interfaces.rest.resources.AreaCoordinatorResource;
import com.hs.hstesis.learning.interfaces.rest.resources.AssignAreaCoordinatorResource;
import com.hs.hstesis.learning.interfaces.rest.resources.ReassignAreaCoordinatorResource;
import com.hs.hstesis.learning.interfaces.rest.transform.AreaCoordinatorResourceFromEntityAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.AssignAreaCoordinatorCommandFromResourceAssembler;
import com.hs.hstesis.learning.interfaces.rest.transform.ReassignAreaCoordinatorCommandFromResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/area-coordinators", produces = MediaType.APPLICATION_JSON_VALUE)
public class AreaCoordinatorController {
    private final AreaCoordinatorCommandService areaCoordinatorCommandService;
    private final AreaCoordinatorQueryService areaCoordinatorQueryService;

    public AreaCoordinatorController(AreaCoordinatorCommandService areaCoordinatorCommandService, AreaCoordinatorQueryService areaCoordinatorQueryService) {
        this.areaCoordinatorCommandService = areaCoordinatorCommandService;
        this.areaCoordinatorQueryService = areaCoordinatorQueryService;
    }

    @PostMapping("/assign")
    public ResponseEntity<AreaCoordinatorResource> assignAreaCoordinator(
            @RequestBody AssignAreaCoordinatorResource assignAreaCoordinatorResource) {
        var assignAreaCoordinatorCommand = AssignAreaCoordinatorCommandFromResourceAssembler.toCommandFromResource(assignAreaCoordinatorResource);
        var areaCoordinatorId = areaCoordinatorCommandService.handle(assignAreaCoordinatorCommand);

        if (areaCoordinatorId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getAreaCoordinatorByIdQuery = new GetAreaCoordinatorByIdQuery(areaCoordinatorId);
        var areaCoordinator = areaCoordinatorQueryService.handle(getAreaCoordinatorByIdQuery);

        if (areaCoordinator.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var areaCoordinatorResource = AreaCoordinatorResourceFromEntityAssembler.toResourceFromEntity(areaCoordinator.get());
        return new ResponseEntity<>(areaCoordinatorResource, HttpStatus.CREATED);
    }

    @PutMapping("/{areaId}/reassign-coordinator")
    public ResponseEntity<AreaCoordinatorResource> reassignAreaCoordinator(
            @PathVariable Long areaId,
            @RequestBody ReassignAreaCoordinatorResource resource) {

        var reassignAreaCoordinatorCommand = ReassignAreaCoordinatorCommandFromResourceAssembler
                .toCommandFromResource(areaId, resource);

        var areaCoordinatorId = areaCoordinatorCommandService.handle(reassignAreaCoordinatorCommand);

        if (areaCoordinatorId == 0L) {
            return ResponseEntity.badRequest().build();
        }

        var getAreaCoordinatorByIdQuery = new GetAreaCoordinatorByIdQuery(areaCoordinatorId);
        var areaCoordinator = areaCoordinatorQueryService.handle(getAreaCoordinatorByIdQuery);

        if (areaCoordinator.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var areaCoordinatorResource = AreaCoordinatorResourceFromEntityAssembler
                .toResourceFromEntity(areaCoordinator.get());

        return ResponseEntity.ok(areaCoordinatorResource);
    }
}
