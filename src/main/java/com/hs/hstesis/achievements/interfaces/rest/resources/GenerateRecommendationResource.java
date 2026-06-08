package com.hs.hstesis.achievements.interfaces.rest.resources;

public record GenerateRecommendationResource(
        String topicName,
        String contextText,
        Integer numQuestions
) {}
