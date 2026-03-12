package com.hs.hstesis.iam.interfaces.rest.resources;

import java.util.List;

public record UserResource(Long id,
                           String name,
                           boolean isActive,
                           List<String> roles) {
}
