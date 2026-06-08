package com.hs.hstesis.iam.domain.model.commands;

import java.util.List;

public record SignUpCommand(
        String name,
        String rawPassword,
        List<String> roles
) {
}
