package com.hs.hstesis.achievements.application.internal.queryservices;

import com.hs.hstesis.achievements.domain.model.valueobjects.AreaPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.ClassroomPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformance;
import com.hs.hstesis.achievements.domain.model.valueobjects.TopicPerformance;
import com.hs.hstesis.achievements.domain.model.queries.GetAreaPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetClassroomPerformanceQuery;
import com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceQuery;
import com.hs.hstesis.achievements.domain.services.AchievementQueryService;
import com.hs.hstesis.achievements.application.internal.outboundservices.acl.ExternalAssessmentService;
import com.hs.hstesis.learning.interfaces.acl.LearningContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AchievementQueryServiceImpl implements AchievementQueryService {

    private final ExternalAssessmentService externalAssessmentService;
    private final LearningContextFacade learningContextFacade;

    public AchievementQueryServiceImpl(ExternalAssessmentService externalAssessmentService, LearningContextFacade learningContextFacade) {
        this.externalAssessmentService = externalAssessmentService;
        this.learningContextFacade = learningContextFacade;
    }

    @Override
    public Optional<StudentPerformance> handle(GetStudentPerformanceQuery query) {
        var submissions = externalAssessmentService.getSubmissionsByStudentId(query.studentId());
        
        if (submissions.isEmpty()) {
            return Optional.of(new StudentPerformance(query.studentId(), "Estudiante " + query.studentId(), 0.0, new ArrayList<>()));
        }

        List<TopicPerformance> topics = new ArrayList<>();
        double totalScore = 0;
        java.util.Map<Long, java.util.List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto>> submissionsByTopic = new java.util.HashMap<>();
        java.util.Map<Long, TopicPerformance> baseTopicInfo = new java.util.HashMap<>();
        java.util.Map<Long, java.util.List<com.hs.hstesis.assessments.interfaces.acl.dto.RemedialTrackingDto>> remedialsByCourse = new java.util.HashMap<>();

        for (var sub : submissions) {
            var questionnaireOpt = externalAssessmentService.getQuestionnaireById(sub.questionnaireId());
            if (questionnaireOpt.isEmpty()) continue;
            var questionnaire = questionnaireOpt.get();

            var topicOpt = learningContextFacade.getTopicByCourseAndGradingPeriodAndOrderIndex(
                    questionnaire.courseId(),
                    questionnaire.gradingPeriodId(),
                    questionnaire.weekNumber()
            );

            long tId = topicOpt.map(t -> t.getId()).orElse(0L);
            if (tId != 0) {
                submissionsByTopic.computeIfAbsent(tId, k -> new ArrayList<>()).add(sub);
                
                if (!baseTopicInfo.containsKey(tId)) {
                    String topicName = topicOpt.map(t -> t.getName()).orElse("Tema Desconocido");
                    baseTopicInfo.put(tId, new TopicPerformance(
                            tId, topicName, questionnaire.weekNumber(),
                            0, 0.0, questionnaire.gradingPeriodId(), questionnaire.courseId(), new ArrayList<>()
                    ));
                }
            }
        }

        for (var entry : submissionsByTopic.entrySet()) {
            long tId = entry.getKey();
            java.util.List<com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto> topicSubmissions = entry.getValue();
            
            // Sort by submittedAt
            topicSubmissions.sort(java.util.Comparator.comparing(com.hs.hstesis.assessments.interfaces.acl.dto.QuestionnaireSubmissionDto::submittedAt));
            
            java.util.List<Double> progressHistory = new ArrayList<>();
            double maxPercentage = 0.0;
            int maxScoreRaw = 0;
            
            for (var sub : topicSubmissions) {
                double percentage = (sub.score() / 20.0) * 100.0;
                progressHistory.add(percentage);
                if (percentage > maxPercentage) {
                    maxPercentage = percentage;
                    maxScoreRaw = sub.score();
                }
            }
            
            var base = baseTopicInfo.get(tId);
            var remedialScore = remedialsByCourse
                    .computeIfAbsent(base.courseId(), courseId -> externalAssessmentService
                            .getRemedialTrackingsByStudentIdAndCourseId(query.studentId(), courseId))
                    .stream()
                    .filter(r -> r.weakTopicId().equals(tId) && r.lastRemedialScore() != null)
                    .mapToInt(r -> r.lastRemedialScore())
                    .max()
                    .orElse(0);
            if (remedialScore > maxScoreRaw) {
                maxScoreRaw = remedialScore;
                maxPercentage = (remedialScore / 20.0) * 100.0;
                progressHistory.add(maxPercentage);
            }
            topics.add(new TopicPerformance(
                    base.topicId(), base.topicName(), base.weekNumber(),
                    maxScoreRaw, maxPercentage, base.gradingPeriodId(), base.courseId(), progressHistory
            ));
        }

        for (TopicPerformance t : topics) {
            totalScore += t.percentage();
        }

        double averageScore = topics.isEmpty() ? 0 : totalScore / topics.size();
        
        return Optional.of(new StudentPerformance(query.studentId(), "Estudiante " + query.studentId(), averageScore, topics));
    }

    @Override
    public Optional<com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary> handle(com.hs.hstesis.achievements.domain.model.queries.GetStudentPerformanceSummaryQuery query) {
        var questionnaires = externalAssessmentService.getQuestionnairesByCourseAndPeriod(query.courseId(), query.gradingPeriodId());
        
        var allSubmissions = externalAssessmentService.getSubmissionsByStudentId(query.studentId());

        var validQuestionnaires = questionnaires.stream()
                .filter(q -> q.status().equals("PUBLISHED") || q.status().equals("CLOSED") || 
                             allSubmissions.stream().anyMatch(s -> s.questionnaireId().equals(q.questionnaireId())))
                .toList();

        if (validQuestionnaires.isEmpty()) {
            return Optional.of(new com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary(
                    query.studentId(), query.gradingPeriodId(), 0.0, List.of(), List.of()
            ));
        }
        
        List<com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary.DefinitiveGrade> definitiveGrades = new ArrayList<>();
        java.util.Map<Integer, List<Integer>> weeklyScores = new java.util.HashMap<>();

        double totalScore = 0.0;

        for (var q : validQuestionnaires) {
            // Find latest submission for this questionnaire
            var latestSubmission = allSubmissions.stream()
                    .filter(s -> s.questionnaireId().equals(q.questionnaireId()))
                    .max(java.util.Comparator.comparing(s -> s.submittedAt()));
            
            // Maintain raw score for definitive grades as requested by user
            int rawScore = latestSubmission.map(s -> s.score()).orElse(0);

            definitiveGrades.add(new com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary.DefinitiveGrade(
                    q.questionnaireId(), q.weekNumber(), rawScore
            ));

            // totalScore and weeklyScores should use percentage to compute average properly
            double percentage = (rawScore / 20.0) * 100.0;
            int percentageInt = (int) Math.round(percentage);

            totalScore += percentageInt;
            weeklyScores.computeIfAbsent(q.weekNumber(), k -> new ArrayList<>()).add(percentageInt);
        }

        double bimesterAverage = validQuestionnaires.isEmpty() ? 0 : totalScore / validQuestionnaires.size();

        List<com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary.WeeklyPerformance> weeklyProgression = new ArrayList<>();
        for (var entry : weeklyScores.entrySet()) {
            double weekAverage = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            weeklyProgression.add(new com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary.WeeklyPerformance(
                    entry.getKey(), weekAverage, weekAverage < 50.0
            ));
        }
        
        weeklyProgression.sort(java.util.Comparator.comparing(w -> w.weekNumber()));

        return Optional.of(new com.hs.hstesis.achievements.domain.model.valueobjects.StudentPerformanceSummary(
                query.studentId(), query.gradingPeriodId(), bimesterAverage, weeklyProgression, definitiveGrades
        ));
    }

    @Override
    public Optional<ClassroomPerformance> handle(GetClassroomPerformanceQuery query) {
        var students = learningContextFacade.getStudentsByClassroom(query.classroomId());
        var classroomCourseId = learningContextFacade.getCourseIdByClassroomId(query.classroomId()).orElse(null);
        
        if (students.isEmpty()) {
            return Optional.of(new ClassroomPerformance(query.classroomId(), "Aula " + query.classroomId(), 0.0, List.of()));
        }

        List<StudentPerformance> studentPerformances = new ArrayList<>();
        double totalAverageScore = 0.0;

        for (var student : students) {
            var perfOpt = handle(new GetStudentPerformanceQuery(student.userId()));
            
            if (perfOpt.isPresent()) {
                var perf = perfOpt.get();
                // Overwrite the generic student name with the actual name
                var classroomTopics = perf.topics().stream()
                        .filter(topic -> classroomCourseId == null || java.util.Objects.equals(topic.courseId(), classroomCourseId))
                        .toList();
                double studentAverage = classroomTopics.stream()
                        .mapToDouble(TopicPerformance::percentage)
                        .average()
                        .orElse(0.0);
                var actualPerf = new StudentPerformance(perf.studentId(), student.userName(), studentAverage, classroomTopics);
                studentPerformances.add(actualPerf);
                totalAverageScore += actualPerf.averageScore();
            } else {
                // If they have no submissions, they get 0 average
                var emptyPerf = new StudentPerformance(student.userId(), student.userName(), 0.0, List.of());
                studentPerformances.add(emptyPerf);
            }
        }

        double classroomAverage = studentPerformances.isEmpty() ? 0.0 : totalAverageScore / studentPerformances.size();
        
        return Optional.of(new ClassroomPerformance(query.classroomId(), "Aula " + query.classroomId(), classroomAverage, studentPerformances));
    }

    @Override
    public Optional<AreaPerformance> handle(GetAreaPerformanceQuery query) {
        var areaOpt = learningContextFacade.getAreaById(query.areaId());
        if (areaOpt.isEmpty()) return Optional.empty();

        var area = areaOpt.get();
        var courses = learningContextFacade.getCoursesByAreaId(query.areaId());
        
        List<ClassroomPerformance> allClassroomsPerformance = new ArrayList<>();
        double totalAverageScore = 0.0;
        int validClassrooms = 0;

        for (var course : courses) {
            var classrooms = learningContextFacade.getClassroomsByCourseId(course.getId());
            for (var classroom : classrooms) {
                var classroomPerfOpt = handle(new GetClassroomPerformanceQuery(classroom.getId()));
                if (classroomPerfOpt.isPresent() && !classroomPerfOpt.get().students().isEmpty()) {
                    var cp = classroomPerfOpt.get();
                    allClassroomsPerformance.add(cp);
                    totalAverageScore += cp.averageScore();
                    validClassrooms++;
                }
            }
        }

        double areaAverage = validClassrooms > 0 ? totalAverageScore / validClassrooms : 0.0;
        return Optional.of(new AreaPerformance(query.areaId(), area.area().getName(), areaAverage, allClassroomsPerformance));
    }
}
