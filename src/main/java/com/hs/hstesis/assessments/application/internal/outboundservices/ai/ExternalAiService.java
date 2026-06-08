package com.hs.hstesis.assessments.application.internal.outboundservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ExternalAiService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.api-key:}")
    private String apiKey;

    public ExternalAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GenerateQuizResponse generateQuiz(String contextText, String topicName, int numQuestions, boolean isRemedial) {
        String url = aiServiceUrl + "/generate-quiz";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        GenerateQuizRequest requestBody = new GenerateQuizRequest(contextText, topicName, numQuestions, isRemedial);
        HttpEntity<GenerateQuizRequest> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.postForObject(url, entity, GenerateQuizResponse.class);
    }

    public record GenerateQuizRequest(String context_text, String topic_name, int num_questions, boolean is_remedial) {}

    public record QuizQuestion(String text, List<String> options, int correctOptionIndex, boolean isRemedial) {}

    public record GenerateQuizResponse(List<QuizQuestion> questions) {}
    
    public List<QuestionFeedbackResponse> generateFeedback(List<WrongQuestionFeedbackDto> wrongAnswers) {
        String url = aiServiceUrl + "/generate-feedback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        GenerateFeedbackRequest requestBody = new GenerateFeedbackRequest(wrongAnswers);
        HttpEntity<GenerateFeedbackRequest> entity = new HttpEntity<>(requestBody, headers);

        GenerateFeedbackResponse response = restTemplate.postForObject(url, entity, GenerateFeedbackResponse.class);
        return response != null ? response.feedbacks() : List.of();
    }

    public record WrongQuestionFeedbackDto(Long question_id, String topic, String question_text, String student_answer, String correct_answer) {}
    
    public record GenerateFeedbackRequest(List<WrongQuestionFeedbackDto> wrong_answers) {}
    
    public record QuestionFeedbackResponse(Long questionId, String feedback) {}

    public record GenerateFeedbackResponse(List<QuestionFeedbackResponse> feedbacks) {}
}
