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
import java.util.List;
import com.hs.hstesis.iam.interfaces.acl.IamContextFacade;
import com.hs.hstesis.achievements.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalRepoService;
import com.hs.hstesis.repo.interfaces.acl.dto.DocumentBasicData;
import com.hs.hstesis.achievements.infrastructure.persistence.jpa.repositories.AchievementInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementCommandServiceImpl implements AchievementCommandService {

    private final AchievementInsightRepository achievementInsightRepository;
    private final AchievementQueryService achievementQueryService;
    private final ExternalAiService externalAiService;
    private final ExternalLearningService externalLearningService;
    private final ExternalRepoService externalRepoService;
    private final IamContextFacade iamContextFacade;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AchievementCommandServiceImpl(AchievementInsightRepository achievementInsightRepository,
                                         AchievementQueryService achievementQueryService,
                                         ExternalAiService externalAiService,
                                         ExternalLearningService externalLearningService,
                                         ExternalRepoService externalRepoService,
                                         IamContextFacade iamContextFacade) {
        this.achievementInsightRepository = achievementInsightRepository;
        this.achievementQueryService = achievementQueryService;
        this.externalAiService = externalAiService;
        this.externalLearningService = externalLearningService;
        this.externalRepoService = externalRepoService;
        this.iamContextFacade = iamContextFacade;
    }


    @Override
    public AchievementInsight handle(GenerateStudentInsightCommand command) {
        var performanceOpt = achievementQueryService.handle(new GetStudentPerformanceQuery(command.studentId()));
        if (performanceOpt.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de rendimiento para este estudiante.");
        }

        try {
            String studentCode = iamContextFacade.fetchUserCodeById(command.studentId())
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el código del estudiante."));
            var performance = performanceOpt.get();
            String jsonData = objectMapper.writeValueAsString(new com.hs.hstesis.achievements.domain.model.valueobjects.StudentInsightData(
                    studentCode, performance.averageScore(), performance.topics()));
            List<Long> courseIds = externalLearningService.getEnrolledCourseIds(command.studentId());
            List<DocumentBasicData> documents = courseIds.isEmpty() ? List.of()
                    : externalRepoService.getDocumentsByCourseIds(courseIds);
            
            String insightText = externalAiService.generateInsight(jsonData, "estudiante", studentCode, documents);
            
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
            List<Long> courseIds = externalLearningService.getCourseIdByClassroomId(command.classroomId())
                    .map(List::of).orElse(List.of());
            List<DocumentBasicData> documents = courseIds.isEmpty() ? List.of()
                    : externalRepoService.getDocumentsByCourseIds(courseIds);
            
            String insightText = externalAiService.generateInsight(jsonData, "profesor", null, documents);
            
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
            List<Long> courseIds = externalLearningService.getCoursesByAreaId(command.areaId());
            List<DocumentBasicData> documents = courseIds.isEmpty() ? List.of()
                    : externalRepoService.getDocumentsByCourseIds(courseIds);
            
            String insightText = externalAiService.generateInsight(jsonData, "coordinador académico", null, documents);
            
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
