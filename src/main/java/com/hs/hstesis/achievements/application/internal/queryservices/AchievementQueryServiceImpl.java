package com.hs.hstesis.achievements.application.internal.queryservices;

import com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.TopicPerformance;
import com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.assessments.interfaces.acl.AssessmentsContextFacade;
import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AchievementQueryServiceImpl implements AchievementQueryService {

    private final AssessmentsContextFacade assessmentsContextFacade;
    private final LearningContextFacade learningContextFacade;

    public AchievementQueryServiceImpl(AssessmentsContextFacade assessmentsContextFacade, LearningContextFacade learningContextFacade) {
        this.assessmentsContextFacade = assessmentsContextFacade;
        this.learningContextFacade = learningContextFacade;
    }

    @Override
    public Optional<StudentPerformance> handle(GetStudentPerformanceQuery query) {
        var submissions = assessmentsContextFacade.getSubmissionsByStudentId(query.studentId());
        
        if (submissions.isEmpty()) {
            return Optional.empty();
        }

        List<TopicPerformance> topics = new ArrayList<>();
        double totalScore = 0;

        for (var sub : submissions) {
            var questionnaire = sub.getQuestionnaireInstance().getQuestionnaire();
            var topicOpt = learningContextFacade.getTopicByCourseAndGradingPeriodAndOrderIndex(
                    questionnaire.getCourseId(),
                    questionnaire.getGradingPeriodId(),
                    questionnaire.getWeekNumber()
            );

            String topicName = topicOpt.map(t -> t.getName()).orElse("Tema Desconocido");
            
            double maxScore = questionnaire.getQuestionsPerAttempt() > 0 ? questionnaire.getQuestionsPerAttempt() : 20.0;
            // Ensure score is valid percentage
            double percentage = (sub.getScore() / maxScore) * 100.0;
            
            topics.add(new TopicPerformance(
                    topicOpt.map(t -> t.getId()).orElse(0L),
                    topicName,
                    questionnaire.getWeekNumber(),
                    sub.getScore(),
                    percentage
            ));
            totalScore += percentage;
        }

        double averageScore = totalScore / submissions.size();
        
        return Optional.of(new StudentPerformance(query.studentId(), "Estudiante " + query.studentId(), averageScore, topics));
    }

    @Override
    public Optional<ClassroomPerformance> handle(GetClassroomPerformanceQuery query) {
        // Mocked aggregation for now, as we need getStudentsByClassroom in LearningContextFacade
        return Optional.of(new ClassroomPerformance(query.classroomId(), "Aula " + query.classroomId(), 85.0, List.of()));
    }

    @Override
    public Optional<AreaPerformance> handle(GetAreaPerformanceQuery query) {
        // Mocked aggregation for now
        return Optional.of(new AreaPerformance(query.areaId(), "Area " + query.areaId(), 85.0, List.of()));
    }
}
