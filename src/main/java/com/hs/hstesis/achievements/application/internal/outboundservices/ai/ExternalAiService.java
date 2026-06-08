package com.hs.hstesis.achievements.application.internal.outboundservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service("achievementsExternalAiService")
public class ExternalAiService {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.api-key:}")
    private String apiKey;

    public ExternalAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateInsight(String performanceJsonData, String targetAudience) {
        String url = aiServiceUrl + "/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        String prompt = String.format(
            "Eres un analista académico experto. A continuación te proveo los datos de rendimiento en formato JSON. " +
            "Genera un insight analítico breve y útil (máximo 3 párrafos) dirigido a un %s. " +
            "Destaca los puntos fuertes y los temas donde se necesita refuerzo.\n\nDatos:\n%s",
            targetAudience, performanceJsonData
        );

        GenerateRequest requestBody = new GenerateRequest(
                List.of(), // no context chunks
                List.of(new Message("user", prompt)),
                1000,
                0.7
        );

        HttpEntity<GenerateRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            GenerateResponse response = restTemplate.postForObject(url, entity, GenerateResponse.class);
            return response != null ? response.answer() : "No se pudo generar el insight.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error de conexión con el motor de Inteligencia Artificial.";
        }
    }

    public GenerateQuizResponse generateRecommendedQuiz(String contextText, String topicName, int numQuestions) {
        String url = aiServiceUrl + "/generate-quiz";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        GenerateQuizRequest requestBody = new GenerateQuizRequest(contextText, topicName, numQuestions, true);
        HttpEntity<GenerateQuizRequest> entity = new HttpEntity<>(requestBody, headers);

        return restTemplate.postForObject(url, entity, GenerateQuizResponse.class);
    }

    public record Message(String role, String content) {}
    public record GenerateRequest(List<String> context_chunks, List<Message> messages, int max_tokens, double temperature) {}
    public record GenerateResponse(String model, String answer, Integer prompt_eval_count, Integer eval_count) {}
    public record GenerateQuizRequest(String context_text, String topic_name, int num_questions, boolean is_remedial) {}
    public record QuizQuestion(String text, List<String> options, int correctOptionIndex, boolean isRemedial) {}
    public record GenerateQuizResponse(List<QuizQuestion> questions) {}
}
