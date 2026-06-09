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

    public String generateInsight(String performanceJsonData, String targetAudience, String docsContext) {
        String url = aiServiceUrl + "/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        String prompt = String.format(
            "Eres un analista académico experto (Sery). A continuación te proveo los datos de rendimiento en formato JSON. " +
            "Genera un insight analítico breve y útil (máximo 3 párrafos) dirigido a un %s. " +
            "Destaca los puntos fuertes y los temas donde se necesita refuerzo. " +
            "IMPORTANTE: Si eres dirigido a un 'profesor' y detectas que hay alumnos con bajo rendimiento en ciertos temas (ej. menor a 60%%), " +
            "debes ser PROACTIVO y sugerirle directamente al profesor la creación de un cuestionario complementario para esos alumnos o temas.\n" +
            "Ademas, tienes a tu disposición la siguiente lista de documentos de la asignatura. Para los temas donde el rendimiento sea bajo, recomienda al usuario leer los documentos correspondientes utilizando ESTE FORMATO EXACTO: **Fuente:** [Nombre del Documento] **Enlace de descarga:** [URL].\n\nDatos de Rendimiento:\n%s\n\n%s",
            targetAudience, performanceJsonData, docsContext
        );

        GenerateRequest requestBody = new GenerateRequest(
                List.of("DATOS DE RENDIMIENTO A ANALIZAR: " + performanceJsonData, docsContext), // add data to context chunks
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
