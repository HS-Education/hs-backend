package com.hs.hstesis.iam.interfaces.rest;

import com.hs.hstesis.iam.domain.model.queries.GetAllUsersQuery;
import com.hs.hstesis.iam.domain.services.UserQueryService;
import com.hs.hstesis.iam.interfaces.rest.resources.UserResource;
import com.hs.hstesis.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    public final UserQueryService userQueryService;

    public UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var getAllUsersQuery = new GetAllUsersQuery();
        var users = userQueryService.handle(getAllUsersQuery);
        var userResources = users.stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok().body(userResources);
    }
}
