package com.hs.hstesis.assessments.application.internal.commandservices;

import com.hs.hstesis.assessments.domain.model.commands.GenerateQuestionnaireCommand;
import com.hs.hstesis.assessments.domain.services.QuestionnaireCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalRepoService;
import com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire;
import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireInstanceRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireRepository;
import com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.RemedialTrackingRepository;

import java.util.List;

@Service
public class QuestionnaireCommandServiceImpl implements QuestionnaireCommandService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireInstanceRepository instanceRepository;
    private final QuestionRepository questionRepository;
    private final RemedialTrackingRepository remedialTrackingRepository;
    private final com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository;
    private final ExternalLearningService externalLearningService;
    private final ExternalRepoService externalRepoService;
    private final ExternalAiService externalAiService;

    public QuestionnaireCommandServiceImpl(QuestionnaireRepository questionnaireRepository,
                                           QuestionnaireInstanceRepository instanceRepository,
                                           QuestionRepository questionRepository,
                                           RemedialTrackingRepository remedialTrackingRepository,
                                           com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository,
                                           ExternalLearningService externalLearningService,
                                           ExternalRepoService externalRepoService,
                                           ExternalAiService externalAiService) {
        this.questionnaireRepository = questionnaireRepository;
        this.instanceRepository = instanceRepository;
        this.questionRepository = questionRepository;
        this.remedialTrackingRepository = remedialTrackingRepository;
        this.submissionRepository = submissionRepository;
        this.externalLearningService = externalLearningService;
        this.externalRepoService = externalRepoService;
        this.externalAiService = externalAiService;
    }

    @Override
    @Transactional
    public void handle(GenerateQuestionnaireCommand command) {
        var existing = questionnaireRepository.findByCourseIdAndGradingPeriodIdAndWeekNumber(
                command.courseId(), command.gradingPeriodId(), command.weekNumber());

        if (existing.isPresent()) {
            throw new IllegalStateException("A questionnaire already exists for this week.");
        }

        var topic = externalLearningService.getTopicByCourseAndGradingPeriodAndOrderIndex(command.courseId(), command.gradingPeriodId(), command.weekNumber())
                .orElseThrow(() -> new IllegalArgumentException("No topic found for the specified week number."));

        int attempts = command.allowedAttempts() != null ? command.allowedAttempts() : 1;
        if (attempts < 1) attempts = 1;
        if (attempts > 3) attempts = 3;

        int questionsPerAttempt = command.questionsPerAttempt() != null ? command.questionsPerAttempt() : 10;
        if (questionsPerAttempt < 1) questionsPerAttempt = 1;
        if (questionsPerAttempt > 10) questionsPerAttempt = 10;

        int totalQuestionsToGenerate = questionsPerAttempt * 3;

        var chunks = externalRepoService.getDocumentChunksByTopicIds(List.of(topic.getId()));
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No document chunks found for the topic.");
        }

        String contextText = String.join("\n\n", chunks);

        var aiResponse = externalAiService.generateQuiz(contextText, topic.getName(), totalQuestionsToGenerate, false);

        var questionnaire = new Questionnaire(command.courseId(), command.gradingPeriodId(), command.weekNumber(), attempts, questionsPerAttempt);
        questionnaireRepository.save(questionnaire);

        var baseInstance = new QuestionnaireInstance(questionnaire, null);
        instanceRepository.save(baseInstance);

        for (var q : aiResponse.questions()) {
            var question = new Question(baseInstance, topic.getId(), q.text(), q.options(), q.correctOptionIndex(), false);
            questionRepository.save(question);
        }
    }

    @Override
    @Transactional
    public void handle(com.hs.hstesis.assessments.domain.model.commands.CreateDirectQuestionnaireCommand command) {
        var existing = questionnaireRepository.findByCourseIdAndGradingPeriodIdAndWeekNumber(
                command.courseId(), command.gradingPeriodId(), command.weekNumber());

        if (existing.isPresent()) {
            throw new IllegalStateException("A questionnaire already exists for this week.");
        }

        var topic = externalLearningService.getTopicByCourseAndGradingPeriodAndOrderIndex(command.courseId(), command.gradingPeriodId(), command.weekNumber())
                .orElseThrow(() -> new IllegalArgumentException("No topic found for the specified week number."));

        int attempts = command.allowedAttempts() != null ? command.allowedAttempts() : 1;
        if (attempts < 1) attempts = 1;
        if (attempts > 3) attempts = 3;

        int questionsPerAttempt = command.questionsPerAttempt() != null ? command.questionsPerAttempt() : 10;
        if (questionsPerAttempt < 1) questionsPerAttempt = 1;
        if (questionsPerAttempt > 10) questionsPerAttempt = 10;

        var questionnaire = new Questionnaire(command.courseId(), command.gradingPeriodId(), command.weekNumber(), attempts, questionsPerAttempt);
        questionnaireRepository.save(questionnaire);

        var baseInstance = new QuestionnaireInstance(questionnaire, null);
        instanceRepository.save(baseInstance);

        for (var q : command.questions()) {
            var question = new Question(baseInstance, topic.getId(), q.text(), q.options(), q.correctOptionIndex(), false);
            questionRepository.save(question);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void handle(com.hs.hstesis.assessments.domain.model.commands.SubmitQuestionnaireCommand command) {
        var instance = instanceRepository.findById(command.questionnaireInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire instance not found."));

        if (instance.getStudentId() == null) {
            throw new IllegalArgumentException("Cannot submit base template questionnaire.");
        }

        var existingSubmission = submissionRepository.findByQuestionnaireInstanceIdAndStudentId(instance.getId(), instance.getStudentId());
        if (existingSubmission.isPresent()) {
            throw new IllegalArgumentException("Questionnaire instance is already submitted.");
        }

        var questions = questionRepository.findAllByQuestionnaireInstanceId(instance.getId());

        int correctAnswers = 0;
        java.util.Set<Long> failedTopics = new java.util.HashSet<>();
        java.util.List<com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService.WrongQuestionFeedbackDto> wrongAnswersList = new java.util.ArrayList<>();
        
        var submission = new com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission(instance, instance.getStudentId(), 0);
        java.util.Map<Long, com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmissionAnswer> answerMap = new java.util.HashMap<>();

        for (var question : questions) {
            Integer submittedAnswer = command.answers().get(question.getId());
            boolean isCorrect = false;
            
            if (submittedAnswer != null && submittedAnswer.equals(question.getCorrectOptionIndex())) {
                correctAnswers++;
                isCorrect = true;
                if (question.getIsRemedial()) {
                    var trackings = remedialTrackingRepository.findAllByCourseIdAndIsResolvedFalse(instance.getQuestionnaire().getCourseId());
                    for (var tracking : trackings) {
                        if (tracking.getStudentId().equals(instance.getStudentId()) && tracking.getWeakTopicId().equals(question.getTopicId())) {
                            tracking.markAsResolved();
                            remedialTrackingRepository.save(tracking);
                        }
                    }
                }
            } else if (submittedAnswer != null) {
                if (!question.getIsRemedial()) {
                    failedTopics.add(question.getTopicId());
                }
                String studentAns = submittedAnswer < question.getOptions().size() ? question.getOptions().get(submittedAnswer) : "Desconocido";
                String correctAns = question.getCorrectOptionIndex() < question.getOptions().size() ? question.getOptions().get(question.getCorrectOptionIndex()) : "Desconocido";
                wrongAnswersList.add(new com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService.WrongQuestionFeedbackDto(
                        question.getId(), "Topic " + question.getTopicId(), question.getText(), studentAns, correctAns
                ));
            }
            
            if (submittedAnswer != null) {
                var ans = submission.addAnswer(question, submittedAnswer, isCorrect);
                answerMap.put(question.getId(), ans);
            }
        }

        int score = (int) Math.round(((double) correctAnswers / questions.size()) * 20.0);
        submission.setScore(score);
        
        if (!wrongAnswersList.isEmpty()) {
            var feedbacks = externalAiService.generateFeedback(wrongAnswersList);
            for (var fb : feedbacks) {
                var ans = answerMap.get(fb.questionId());
                if (ans != null) {
                    ans.setAiFeedback(fb.feedback());
                }
            }
        }

        submissionRepository.save(submission);

        if (score < 13) {
            for (Long topicId : failedTopics) {
                var tracking = new com.hs.hstesis.assessments.domain.model.entities.RemedialTracking(
                        instance.getStudentId(),
                        instance.getQuestionnaire().getCourseId(),
                        instance.getQuestionnaire().getWeekNumber(),
                        topicId
                );
                remedialTrackingRepository.save(tracking);
            }
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Long handle(com.hs.hstesis.assessments.domain.model.commands.StartQuestionnaireCommand command) {
        var questionnaire = questionnaireRepository.findById(command.questionnaireId())
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire not found."));

        var enrolledCourseIds = externalLearningService.getEnrolledCourseIds(command.studentId());
        if (!enrolledCourseIds.contains(questionnaire.getCourseId())) {
            throw new org.springframework.security.access.AccessDeniedException("Student is not enrolled in this course.");
        }

        var existingInstances = instanceRepository.findAllByQuestionnaireIdAndStudentId(command.questionnaireId(), command.studentId());
        
        // Check if there is already an unfinished (STARTED) instance
        var unfinished = existingInstances.stream()
                .filter(i -> submissionRepository.findByQuestionnaireInstanceIdAndStudentId(i.getId(), command.studentId()).isEmpty())
                .findFirst();
                
        if (unfinished.isPresent()) {
            return unfinished.get().getId();
        }
        
        if (existingInstances.size() >= questionnaire.getAllowedAttempts()) {
            throw new IllegalStateException("You have reached the maximum number of attempts for this questionnaire.");
        }

        var studentInstance = new QuestionnaireInstance(questionnaire, command.studentId());
        instanceRepository.save(studentInstance);

        var baseInstance = instanceRepository.findByQuestionnaireIdAndStudentIdIsNull(command.questionnaireId())
                .orElseThrow(() -> new IllegalStateException("Base template for this questionnaire not found."));

        var allBaseQuestions = questionRepository.findAllByQuestionnaireInstanceId(baseInstance.getId());
        
        java.util.Collections.shuffle(allBaseQuestions);
        var selectedQuestions = allBaseQuestions.stream()
                .limit(questionnaire.getQuestionsPerAttempt())
                .collect(java.util.stream.Collectors.toList());

        for (var baseQ : selectedQuestions) {
            var clone = new Question(studentInstance, baseQ.getTopicId(), baseQ.getText(), baseQ.getOptions(), baseQ.getCorrectOptionIndex(), false);
            questionRepository.save(clone);
        }

        var remedials = remedialTrackingRepository.findAllByCourseIdAndIsResolvedFalse(questionnaire.getCourseId());
        var studentRemedials = remedials.stream()
                .filter(r -> r.getStudentId().equals(command.studentId()))
                .collect(java.util.stream.Collectors.toList());

        for (var rem : studentRemedials) {
            var weakTopicChunks = externalRepoService.getDocumentChunksByTopicIds(List.of(rem.getWeakTopicId()));
            if (!weakTopicChunks.isEmpty()) {
                String weakContext = String.join("\n\n", weakTopicChunks);
                var remResponse = externalAiService.generateQuiz(weakContext, "Remedial Topic", 2, true);
                
                for (var rq : remResponse.questions()) {
                    var rQuestion = new Question(studentInstance, rem.getWeakTopicId(), rq.text(), rq.options(), rq.correctOptionIndex(), true);
                    questionRepository.save(rQuestion);
                }
            }
        }

        return studentInstance.getId();
    }
}
