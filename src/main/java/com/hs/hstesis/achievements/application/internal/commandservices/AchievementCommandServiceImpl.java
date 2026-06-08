package com.hs.hstesis.achievements.application.internal.commandservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.hstesis.achievements.domain.model.aggregates.AchievementInsight;
import com.hs.hstesis.achievements.domain.services.AchievementCommandService;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.domain.model.commands.GenerateAreaInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomInsightCommand;
import com.hs.hstesis.achievements.domain.model.commands.GenerateStudentInsightCommand;
import com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementCommandServiceImpl implements AchievementCommandService {

    private final AchievementInsightRepository achievementInsightRepository;
    private final AchievementQueryService achievementQueryService;
    private final ExternalAiService externalAiService;
    private final ObjectMapper objectMapper;

    public AchievementCommandServiceImpl(AchievementInsightRepository achievementInsightRepository,
                                         AchievementQueryService achievementQueryService,
                                         ExternalAiService externalAiService,
                                         ObjectMapper objectMapper) {
        this.achievementInsightRepository = achievementInsightRepository;
        this.achievementQueryService = achievementQueryService;
        this.externalAiService = externalAiService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AchievementInsight handle(GenerateStudentInsightCommand command) {
        var performanceOpt = achievementQueryService.handle(new GetStudentPerformanceQuery(command.studentId()));
        if (performanceOpt.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de rendimiento para este estudiante.");
        }

        try {
            String jsonData = objectMapper.writeValueAsString(performanceOpt.get());
            String insightText = externalAiService.generateInsight(jsonData, "estudiante");
            
            var insight = new AchievementInsight("STUDENT", command.studentId(), insightText);
            return achievementInsightRepository.save(insight);
        } catch (Exception e) {
            throw new RuntimeException("Error generando insight para estudiante", e);
        }
    }

    @Override
    @Transactional
    public AchievementInsight handle(GenerateClassroomInsightCommand command) {
        var performanceOpt = achievementQueryService.handle(new GetClassroomPerformanceQuery(command.classroomId()));
        if (performanceOpt.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de rendimiento para este aula.");
        }

        try {
            String jsonData = objectMapper.writeValueAsString(performanceOpt.get());
            String insightText = externalAiService.generateInsight(jsonData, "profesor");
            
            var insight = new AchievementInsight("CLASSROOM", command.classroomId(), insightText);
            return achievementInsightRepository.save(insight);
        } catch (Exception e) {
            throw new RuntimeException("Error generando insight para aula", e);
        }
    }

    @Override
    @Transactional
    public AchievementInsight handle(GenerateAreaInsightCommand command) {
        var performanceOpt = achievementQueryService.handle(new GetAreaPerformanceQuery(command.areaId()));
        if (performanceOpt.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de rendimiento para esta área.");
        }

        try {
            String jsonData = objectMapper.writeValueAsString(performanceOpt.get());
            String insightText = externalAiService.generateInsight(jsonData, "coordinador académico");
            
            var insight = new AchievementInsight("AREA", command.areaId(), insightText);
            return achievementInsightRepository.save(insight);
        } catch (Exception e) {
            throw new RuntimeException("Error generando insight para área", e);
        }
    }

    @Override
    public ExternalAiService.GenerateQuizResponse handle(com.hs.hstesis.achievements.domain.model.commands.GenerateClassroomRecommendationCommand command) {
        try {
            return externalAiService.generateRecommendedQuiz(command.contextText(), command.topicName(), command.numQuestions());
        } catch (Exception e) {
            throw new RuntimeException("Error generando cuestionario recomendado", e);
        }
    }
}
