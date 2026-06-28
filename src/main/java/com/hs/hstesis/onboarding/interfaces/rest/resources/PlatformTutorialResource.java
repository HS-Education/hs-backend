package com.hs.hstesis.onboarding.interfaces.rest.resources;

import java.util.Date;

public record PlatformTutorialResource(Long id, String title, String description, String fileUrl, Date createdAt) {}

