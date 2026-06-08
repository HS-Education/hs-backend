package com.hs.hstesis.achievements.domain.model.commands;

public record GenerateClassroomRecommendationCommand(Long classroomId, String topicName, String contextText, Integer numQuestions) {}
