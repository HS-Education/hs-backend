package com.hs.hstesis.achievements.domain.services;

import com.hs.hstesis.achievements.domain.model.aggregates.AchievementInsight;
import com.hs.hstesis.achievements.domain.model.commands.GenerateAreaInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateStudentInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomRecommendationCommand;
import com.hs.hstesis.achievements.application.internal.outboundservices.ai.ExternalAiService.GenerateQuizResponse;

public interface AchievementCommandService {
    AchievementInsight handle(GenerateStudentInsightCommand command);
    AchievementInsight handle(GenerateClassroomInsightCommand command);
    AchievementInsight handle(GenerateAreaInsightCommand command);
    GenerateQuizResponse handle(GenerateClassroomRecommendationCommand command);
}
