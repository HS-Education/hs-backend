package com.hs.hstesis.iam.interfaces.rest.resources;

import java.util.List;

public record AuthenticatedUserResource(Long id, String name, String username, List<String> roles) {}
