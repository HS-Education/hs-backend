package com.hs.hstesis.iam.interfaces.rest.resources;

import java.util.List;

public record SignUpResource(
        String name,
        String password,
        List<String> roles
) {
}
