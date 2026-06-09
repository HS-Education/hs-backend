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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
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
            
            // Assumes max score is always 20
            double maxScore = 20.0;
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
        var students = learningContextFacade.getStudentsByClassroom(query.classroomId());
        
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
                var actualPerf = new StudentPerformance(perf.studentId(), student.userName(), perf.averageScore(), perf.topics());
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
