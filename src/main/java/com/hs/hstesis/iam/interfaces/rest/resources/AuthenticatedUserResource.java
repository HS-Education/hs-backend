package com.hs.hstesis.iam.interfaces.rest.resources;

import java.util.List;

public record AuthenticatedUserResource(Long id, String username, List<String> roles) {}
