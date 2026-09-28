package com.hs.hstesis.onboarding.domain.model.commands;
public record UpdatePlatformTutorialCommand(Long tutorialId, String title, String description, String fileUrl) {}
