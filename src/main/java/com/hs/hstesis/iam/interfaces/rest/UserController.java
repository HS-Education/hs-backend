package com.hs.hstesis.iam.interfaces.rest;

import com.hs.hstesis.iam.domain.model.commands.AddRoleToUserCommand;
import com.hs.hstesis.iam.domain.model.commands.RemoveRoleFromUserCommand;
import com.hs.hstesis.iam.domain.model.queries.GetAllUsersQuery;
import com.hs.hstesis.iam.domain.services.UserCommandService;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import com.hs.hstesis.iam.interfaces.rest.resources.UserResource;
import com.hs.hstesis.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.hs.hstesis.shared.interfaces.rest.resources.MessageResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService,
                          UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @Operation(description = "Retrieves a list of all users.")
    @GetMapping
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var getAllUsersQuery = new GetAllUsersQuery();
        var users = userQueryService.handle(getAllUsersQuery);

        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var userResources = users.stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity).
                toList();
        return ResponseEntity.ok().body(userResources);
    }

    @Operation(description = "Adds a role to a user.")
    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<MessageResource> addRole(@PathVariable Long userId, @PathVariable Long roleId) {
        userCommandService.handle(new AddRoleToUserCommand(userId, roleId));
        return ResponseEntity.ok(new MessageResource("Role added successfully"));
    }

    @Operation(description = "Removes a role from a user.")
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<MessageResource> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        userCommandService.handle(new RemoveRoleFromUserCommand(userId, roleId));
        return ResponseEntity.ok(new MessageResource("Role removed successfully"));
    }

    @Operation(description = "Registers a new user and returns the auto-generated username.")
    @PostMapping
    public ResponseEntity<com.hs.hstesis.iam.interfaces.rest.resources.SignUpResponseResource> registerUser(@RequestBody com.hs.hstesis.iam.interfaces.rest.resources.SignUpResource resource) {
        var command = new com.hs.hstesis.iam.domain.model.commands.SignUpCommand(resource.name(), resource.password(), resource.roles());
        String username = userCommandService.handle(command);
        return ResponseEntity.ok(new com.hs.hstesis.iam.interfaces.rest.resources.SignUpResponseResource(username, "Usuario creado exitosamente"));
    }
}
