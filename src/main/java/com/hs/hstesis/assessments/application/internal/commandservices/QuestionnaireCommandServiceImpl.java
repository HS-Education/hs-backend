package com.hs.hstesis.assessments.application.internal.commandservices;

import com.hs.hstesis.assessments.domain.model.commands.GenerateQuestionnaireCommand;
import com.hs.hstesis.assessments.domain.services.QuestionnaireCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalLearningService;
import com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalNotificationService;
import com.hs.hstesis.assessments.application.internal.outboundservices.acl.ExternalRepoService;
import com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService;
import com.hs.hstesis.assessments.domain.model.aggregates.Questionnaire;
import com.hs.hstesis.assessments.domain.model.entities.Question;
import com.hs.hstesis.assessments.domain.model.entities.QuestionnaireInstance;
import com.hs.hstesis.assessments.domain.model.valueobjects.QuestionDifficulty;
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
    private final ExternalNotificationService externalNotificationService;

    public QuestionnaireCommandServiceImpl(QuestionnaireRepository questionnaireRepository,
                                           QuestionnaireInstanceRepository instanceRepository,
                                           QuestionRepository questionRepository,
                                           RemedialTrackingRepository remedialTrackingRepository,
                                           com.hs.hstesis.assessments.infrastructure.persistance.jpa.repositories.QuestionnaireSubmissionRepository submissionRepository,
                                           ExternalLearningService externalLearningService,
                                           ExternalRepoService externalRepoService,
                                           ExternalAiService externalAiService,
                                           ExternalNotificationService externalNotificationService) {
        this.questionnaireRepository = questionnaireRepository;
        this.instanceRepository = instanceRepository;
        this.questionRepository = questionRepository;
        this.remedialTrackingRepository = remedialTrackingRepository;
        this.submissionRepository = submissionRepository;
        this.externalLearningService = externalLearningService;
        this.externalRepoService = externalRepoService;
        this.externalAiService = externalAiService;
        this.externalNotificationService = externalNotificationService;
    }

    @Override
    @Transactional
    public void handle(GenerateQuestionnaireCommand command) {
        if (questionnaireRepository.existsByCourseIdAndGradingPeriodIdAndWeekNumber(
                command.courseId(), command.gradingPeriodId(), command.weekNumber())) {
            throw new IllegalStateException("A questionnaire already exists for the specified week.");
        }

        var topic = externalLearningService.getTopicByCourseAndGradingPeriodAndOrderIndex(command.courseId(), command.gradingPeriodId(), command.weekNumber())
                .orElseThrow(() -> new IllegalArgumentException("No topic found for the specified week number."));

        // Los cuestionarios nuevos se publican con un único intento. Se conserva
        // la columna allowedAttempts para que la política pueda reactivarse después.
        int attempts = 1;

        int questionsPerAttempt = command.questionsPerAttempt() != null ? command.questionsPerAttempt() : 5;
        if (questionsPerAttempt < 5) questionsPerAttempt = 5;
        if (questionsPerAttempt > 10) questionsPerAttempt = 10;
        int bankQuestionCount = 15;

        var chunks = externalRepoService.getDocumentChunksByTopicIds(List.of(topic.getId()));
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No document chunks found for the topic.");
        }

        String contextText = String.join("\n\n", chunks);

        var aiResponse = externalAiService.generateQuiz(contextText, topic.getName(), bankQuestionCount, false);

        if (aiResponse == null || aiResponse.questions() == null || aiResponse.questions().size() != bankQuestionCount) {
            throw new IllegalStateException("AI did not generate the required 15-question bank.");
        }
        long lowQuestions = aiResponse.questions().stream()
                .filter(q -> parseDifficulty(q.difficulty()) == QuestionDifficulty.LOW)
                .count();
        long intermediateQuestions = aiResponse.questions().stream()
                .filter(q -> parseDifficulty(q.difficulty()) == QuestionDifficulty.INTERMEDIATE)
                .count();
        if (lowQuestions < 5 || intermediateQuestions < 10) {
            throw new IllegalStateException("AI question bank must contain 5 LOW and 10 INTERMEDIATE questions.");
        }

        var questionnaire = new Questionnaire(command.courseId(), command.gradingPeriodId(), command.weekNumber(), attempts, questionsPerAttempt);
        questionnaireRepository.save(questionnaire);

        var baseInstance = new QuestionnaireInstance(questionnaire, null);
        instanceRepository.save(baseInstance);

        for (var q : aiResponse.questions()) {
            var originalOptions = q.options();
            var originalCorrectOption = originalOptions.get(q.correctOptionIndex());
            var shuffledOptions = new java.util.ArrayList<>(originalOptions);
            java.util.Collections.shuffle(shuffledOptions);
            var newCorrectIndex = shuffledOptions.indexOf(originalCorrectOption);

            QuestionDifficulty difficulty = parseDifficulty(q.difficulty());
            var question = new Question(baseInstance, topic.getId(), q.text(), shuffledOptions, newCorrectIndex, false, difficulty);
            questionRepository.save(question);
        }

        // Send notifications
        var studentIds = externalLearningService.getStudentIdsByCourseId(command.courseId());
        String msg = String.format("Tienes un nuevo cuestionario disponible para la semana %d en tu curso.", command.weekNumber());
        for (Long sId : studentIds) {
            if (sId.equals(command.actorId())) continue;
            externalNotificationService.sendNotification(
                    sId,
                    msg,
                    com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType.NEW_QUESTIONNAIRE);
        }
    }

    @Override
    @Transactional
    public void handle(com.hs.hstesis.assessments.domain.model.commands.GenerateRemedialQuestionnaireCommand command) {
        int numQuestions = command.numQuestions() != null ? command.numQuestions() : 10;
        var baseQuestions = questionRepository.findBaseQuestionsByTopicId(command.topicId());
        if (baseQuestions.isEmpty()) {
            throw new IllegalStateException("No base questions found for the specified topic.");
        }

        java.util.Collections.shuffle(baseQuestions);

        var questionnaire = new Questionnaire(
                command.courseId(), 
                command.gradingPeriodId(), 
                command.weekNumber(), 
                1, // 1 attempt
                numQuestions, // requested questions
                command.studentId(), 
                com.hs.hstesis.assessments.domain.model.valueobjects.QuestionnaireType.REMEDIAL
        );
        questionnaireRepository.save(questionnaire);

        var baseInstance = new QuestionnaireInstance(questionnaire, null);
        instanceRepository.save(baseInstance);

        for (var baseQuestion : baseQuestions.stream().limit(numQuestions).toList()) {
            var originalOptions = baseQuestion.getOptions();
            var originalCorrectOption = originalOptions.get(baseQuestion.getCorrectOptionIndex());
            var shuffledOptions = new java.util.ArrayList<>(originalOptions);
            java.util.Collections.shuffle(shuffledOptions);
            var newCorrectIndex = shuffledOptions.indexOf(originalCorrectOption);

            var question = new Question(baseInstance, baseQuestion.getTopicId(), baseQuestion.getText(), shuffledOptions,
                    newCorrectIndex, true, effectiveDifficulty(baseQuestion));
            questionRepository.save(question);
        }

        // Send notification
        String msg = "¡Oportunidad de mejora! Se ha generado un nuevo cuestionario de repaso personalizado especialmente para ti.";
        externalNotificationService.sendNotification(
                command.studentId(),
                msg,
                com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType.NEW_QUESTIONNAIRE);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void handle(com.hs.hstesis.assessments.domain.model.commands.SubmitQuestionnaireCommand command) {
        var instance = instanceRepository.findById(command.questionnaireInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire instance not found."));

        if (instance.getStudentId() == null) {
            throw new IllegalArgumentException("Cannot submit base template questionnaire.");
        }

        var existingSubmission = submissionRepository.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(instance.getId(), instance.getStudentId());
        if (existingSubmission.isPresent()) {
            throw new IllegalArgumentException("Questionnaire instance is already submitted.");
        }

        var questions = questionRepository.findAllByQuestionnaireInstanceId(instance.getId());

        int correctAnswers = 0;
        java.util.Map<Long, Integer> regularTotals = new java.util.HashMap<>();
        java.util.Map<Long, Integer> regularCorrect = new java.util.HashMap<>();
        java.util.Map<Long, Integer> remedialTotals = new java.util.HashMap<>();
        java.util.Map<Long, Integer> remedialCorrect = new java.util.HashMap<>();
        java.util.List<com.hs.hstesis.assessments.application.internal.outboundservices.ai.ExternalAiService.WrongQuestionFeedbackDto> wrongAnswersList = new java.util.ArrayList<>();
        
        var submission = new com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmission(instance, instance.getStudentId(), 0);
        java.util.Map<Long, com.hs.hstesis.assessments.domain.model.entities.QuestionnaireSubmissionAnswer> answerMap = new java.util.HashMap<>();

        for (var question : questions) {
            Integer submittedAnswer = command.answers().get(question.getId());
            boolean isCorrect = false;

            var totals = question.getIsRemedial() ? remedialTotals : regularTotals;
            totals.merge(question.getTopicId(), 1, Integer::sum);
            
            if (submittedAnswer != null && submittedAnswer.equals(question.getCorrectOptionIndex())) {
                correctAnswers++;
                isCorrect = true;
                if (question.getIsRemedial()) {
                    remedialCorrect.merge(question.getTopicId(), 1, Integer::sum);
                } else {
                    regularCorrect.merge(question.getTopicId(), 1, Integer::sum);
                }
            } else if (submittedAnswer != null) {
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

        if (score <= 10) {
            var courseId = instance.getQuestionnaire().getCourseId();
            var studentName = externalLearningService
                    .getStudentNameByCourseId(courseId, instance.getStudentId())
                    .orElse("el estudiante con ID " + instance.getStudentId());
            var message = String.format(
                    "Alerta de bajo rendimiento: %s obtuvo %d/20 en un cuestionario del curso.",
                    studentName,
                    score);
            externalLearningService.getTeacherAndCoordinatorIdsByCourseId(courseId).forEach(recipientId ->
                    externalNotificationService.sendNotification(
                            recipientId,
                            message,
                            com.hs.hstesis.notifications.domain.model.valueobjects.NotificationType.LOW_PERFORMANCE));
        }

        for (var entry : remedialTotals.entrySet()) {
            int topicScore = scoreFor(remedialCorrect.getOrDefault(entry.getKey(), 0), entry.getValue());
            updateExistingRemediation(instance.getStudentId(), instance.getQuestionnaire().getCourseId(), entry.getKey(), topicScore);
        }

        for (var entry : regularTotals.entrySet()) {
            int topicScore = scoreFor(regularCorrect.getOrDefault(entry.getKey(), 0), entry.getValue());
            createOrUpdateInitialRemediation(instance.getStudentId(), instance.getQuestionnaire().getCourseId(),
                    instance.getQuestionnaire().getWeekNumber(), entry.getKey(), topicScore);
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
                .filter(i -> submissionRepository.findFirstByQuestionnaireInstanceIdAndStudentIdOrderBySubmittedAtDesc(i.getId(), command.studentId()).isEmpty())
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

        var allBaseQuestions = questionRepository.findAllByQuestionnaireInstanceId(baseInstance.getId()).stream()
                .filter(q -> effectiveDifficulty(q) == QuestionDifficulty.INTERMEDIATE)
                .collect(java.util.stream.Collectors.toList());
        
        java.util.Collections.shuffle(allBaseQuestions);
        var selectedQuestions = allBaseQuestions.stream()
                .limit(questionnaire.getQuestionsPerAttempt())
                .collect(java.util.stream.Collectors.toList());

        for (var baseQ : selectedQuestions) {
            var originalOptions = baseQ.getOptions();
            var originalCorrectOption = originalOptions.get(baseQ.getCorrectOptionIndex());
            var shuffledOptions = new java.util.ArrayList<>(originalOptions);
            java.util.Collections.shuffle(shuffledOptions);
            var newCorrectIndex = shuffledOptions.indexOf(originalCorrectOption);

            var clone = new Question(
                    studentInstance,
                    baseQ.getTopicId(),
                    baseQ.getText(),
                    shuffledOptions,
                    newCorrectIndex,
                    baseQ.getIsRemedial(),
                    effectiveDifficulty(baseQ));
            questionRepository.save(clone);
        }

        var remedials = remedialTrackingRepository.findAllByCourseIdAndIsResolvedFalse(questionnaire.getCourseId());
        var studentRemedials = remedials.stream()
                .filter(r -> r.getStudentId().equals(command.studentId()))
                .collect(java.util.stream.Collectors.toList());

        for (var rem : studentRemedials) {
            var baseRemedialQuestions = questionRepository.findBaseQuestionsByTopicId(rem.getWeakTopicId());
            java.util.Collections.shuffle(baseRemedialQuestions);

            baseRemedialQuestions.stream()
                    .filter(baseQuestion -> effectiveDifficulty(baseQuestion) == rem.effectiveDifficulty())
                    .filter(baseQuestion -> selectedQuestions.stream()
                            .noneMatch(selectedQuestion -> selectedQuestion.getId().equals(baseQuestion.getId())))
                    .limit(rem.questionsToAssign())
                    .forEach(baseQuestion -> {
                        var originalOptions = baseQuestion.getOptions();
                        var originalCorrectOption = originalOptions.get(baseQuestion.getCorrectOptionIndex());
                        var shuffledOptions = new java.util.ArrayList<>(originalOptions);
                        java.util.Collections.shuffle(shuffledOptions);
                        var newCorrectIndex = shuffledOptions.indexOf(originalCorrectOption);

                        var remedialQuestion = new Question(
                                studentInstance,
                                baseQuestion.getTopicId(),
                                baseQuestion.getText(),
                                shuffledOptions,
                                newCorrectIndex,
                                true,
                                effectiveDifficulty(baseQuestion));
                        questionRepository.save(remedialQuestion);
                    });
        }

        return studentInstance.getId();
    }

    private int scoreFor(int correct, int total) {
        return total == 0 ? 0 : (int) Math.round(((double) correct / total) * 20.0);
    }

    private QuestionDifficulty effectiveDifficulty(Question question) {
        return question.getDifficulty() != null ? question.getDifficulty() : QuestionDifficulty.INTERMEDIATE;
    }

    private QuestionDifficulty parseDifficulty(String difficulty) {
        try {
            return QuestionDifficulty.valueOf(difficulty.toUpperCase());
        } catch (Exception ignored) {
            return QuestionDifficulty.INTERMEDIATE;
        }
    }

    private int remediationCountFor(int score) {
        if (score <= 5) return 5;
        if (score <= 10) return 4;
        if (score <= 13) return 3;
        if (score <= 15) return 2;
        if (score <= 17) return 1;
        return 0;
    }

    private QuestionDifficulty remediationDifficultyFor(int score) {
        return score >= 14 ? QuestionDifficulty.INTERMEDIATE : QuestionDifficulty.LOW;
    }

    private void createOrUpdateInitialRemediation(Long studentId, Long courseId, Integer weekNumber,
                                                   Long topicId, int topicScore) {
        var existing = remedialTrackingRepository
                .findFirstByStudentIdAndCourseIdAndWeakTopicIdAndIsResolvedFalse(studentId, courseId, topicId);
        if (topicScore >= 18) {
            existing.ifPresent(tracking -> {
                tracking.markAsResolved();
                tracking.setLastRemedialScore(topicScore);
                remedialTrackingRepository.save(tracking);
            });
            return;
        }

        int count = remediationCountFor(topicScore);
        var difficulty = remediationDifficultyFor(topicScore);
        if (existing.isPresent()) {
            var tracking = existing.get();
            tracking.updateRemediation(topicScore, count, difficulty, count == 1 && difficulty == QuestionDifficulty.INTERMEDIATE);
            remedialTrackingRepository.save(tracking);
        } else {
            var tracking = new com.hs.hstesis.assessments.domain.model.entities.RemedialTracking(
                    studentId, courseId, weekNumber, topicId, count, difficulty);
            tracking.setLastRemedialScore(topicScore);
            tracking.setConsolidationMode(count == 1 && difficulty == QuestionDifficulty.INTERMEDIATE);
            remedialTrackingRepository.save(tracking);
        }
    }

    private void updateExistingRemediation(Long studentId, Long courseId, Long topicId, int remedialScore) {
        remedialTrackingRepository
                .findFirstByStudentIdAndCourseIdAndWeakTopicIdAndIsResolvedFalse(studentId, courseId, topicId)
                .ifPresent(tracking -> {
                    if (remedialScore >= 18) {
                        tracking.setLastRemedialScore(remedialScore);
                        tracking.markAsResolved();
                    } else if (tracking.isInConsolidationMode()) {
                        tracking.updateRemediation(remedialScore, 1, QuestionDifficulty.INTERMEDIATE, true);
                    } else if (remedialScore >= 14) {
                        tracking.updateRemediation(remedialScore, 1, QuestionDifficulty.INTERMEDIATE, true);
                    } else {
                        tracking.updateRemediation(remedialScore, remediationCountFor(remedialScore),
                                QuestionDifficulty.LOW, false);
                    }
                    remedialTrackingRepository.save(tracking);
                });
    }
}
