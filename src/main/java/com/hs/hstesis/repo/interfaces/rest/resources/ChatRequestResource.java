package com.hs.hstesis.repo.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestResource(
        @NotBlank(message = "Question cannot be blank")
        String question
) {}
