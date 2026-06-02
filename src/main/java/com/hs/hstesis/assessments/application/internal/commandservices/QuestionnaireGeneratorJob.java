package com.hs.hstesis.assessments.application.internal.commandservices;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireGeneratorJob {

    // This job will run every Saturday at 00:00 (for example)
    @Scheduled(cron = "0 0 0 * * SAT")
    public void generateWeeklyQuestionnaires() {
        // Logic to trigger the questionnaire generation for all active courses
        // This will be implemented in Phase 4
    }
}
