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

        String audienceInstructions = "";
        if ("estudiante".equals(targetAudience)) {
            audienceInstructions = "Si el estudiante tiene calificaciones bajas (menores o iguales a 13 o 60%), debes darle recomendaciones globales directas: dile qué temas exactos debe estudiar, qué conceptos debe repasar y dale sugerencias prácticas de mejora que él podría aplicar. Por el contrario, si el estudiante tiene calificaciones altas (mayores o iguales a 16 u 80%), dale recomendaciones muy positivas, motívalo a seguir así y explícitamente NO recomiendes cuestionarios de repaso para estos casos sobresalientes.";
        } else if ("profesor".equals(targetAudience)) {
            audienceInstructions = "Si hay alumnos con bajas calificaciones (menores o iguales a 13 o 60%), sugiere qué temas deben reforzar y RECUÉRDALE AL PROFESOR explícitamente que tiene la opción en el sistema de generar un 'Cuestionario de Repaso' personalizado para esos alumnos en esos temas. Sin embargo, para los alumnos con notas altas (mayores o iguales a 16 u 80%), dale recomendaciones positivas al profesor y aclara que a ellos NO se les debe generar cuestionarios de repaso.";
        } else if ("coordinador académico".equals(targetAudience)) {
            audienceInstructions = "Tus recomendaciones deben ser de observación directiva. Si el aula tiene buen rendimiento (promedio mayor o igual a 16 u 80%), felicita indicando algo como 'Dale un ojo a esta aula que está TOP, excelente trabajo' y aclara que no requieren intervención con cuestionarios. Si el aula o áreas están mal, advierte diciendo 'Ponle el ojo a esta aula/área, están bajos' y sugiere estrategias paulatinas de mejora.";
        }

        String prompt = String.format(
            "Eres un analista académico experto (Sery). A continuación te proveo los datos de rendimiento en formato JSON. " +
            "Genera un insight analítico breve y útil (máximo 3 párrafos) dirigido a un %s. " +
            "Destaca los puntos fuertes y los temas donde se necesita refuerzo.\n" +
            "INSTRUCCIONES ESPECÍFICAS SEGÚN TU AUDIENCIA:\n%s\n\n" +
            "Además, tienes a tu disposición la siguiente lista de documentos de la asignatura. Para los temas donde el rendimiento sea bajo, recomienda leer los documentos correspondientes utilizando ESTE FORMATO EXACTO: **Fuente:** [Nombre del Documento] **Enlace de descarga:** [URL].\n\nDatos de Rendimiento:\n%s\n\n%s",
            targetAudience, audienceInstructions, performanceJsonData, docsContext
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
