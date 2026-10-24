package com.hs.hstesis.achievements.application.internal.outboundservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import com.hs.hstesis.repo.interfaces.acl.dto.DocumentBasicData;
import com.hs.hstesis.shared.infrastructure.ai.DocumentReferenceFormatter;

@Service("achievementsExternalAiService")
public class ExternalAiService {
    private static final Logger log = LoggerFactory.getLogger(ExternalAiService.class);

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.api-key:}")
    private String apiKey;

    public ExternalAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateInsight(String performanceJsonData, String targetAudience, String studentCode, List<DocumentBasicData> documents) {
        String url = aiServiceUrl + "/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!apiKey.isEmpty()) {
            headers.set("X-API-Key", apiKey);
        }

        String audienceInstructions = "";
        if ("estudiante".equals(targetAudience)) {
            audienceInstructions = "Identifica al estudiante únicamente mediante su código de usuario (" + studentCode + "). No solicites, inventes ni reveles su nombre. Si el estudiante tiene calificaciones bajas (menores o iguales a 13 o 60%), debes darle recomendaciones globales directas: dile qué temas exactos debe estudiar, qué conceptos debe repasar y dale sugerencias prácticas de mejora que él podría aplicar. Por el contrario, si el estudiante tiene calificaciones altas (mayores o iguales a 16 u 80%), dale recomendaciones muy positivas, motívalo a seguir así y explícitamente NO recomiendes cuestionarios de repaso para estos casos sobresalientes.";
        } else if ("profesor".equals(targetAudience)) {
            audienceInstructions = "Si hay alumnos con bajas calificaciones (menores o iguales a 13 o 60%), indica qué temas y conceptos deben reforzar y sugiere estrategias, actividades o materiales de apoyo que el profesor pueda aplicar. Para los alumnos con notas altas (mayores o iguales a 16 u 80%), ofrece recomendaciones positivas para mantener su progreso. No sugieras crear, gestionar ni asignar cuestionarios adicionales o de repaso.";
        } else if ("coordinador académico".equals(targetAudience)) {
            audienceInstructions = "Tus recomendaciones deben ser de observación directiva. Si el aula tiene buen rendimiento (promedio mayor o igual a 16 u 80%), felicita indicando algo como 'Dale un ojo a esta aula que está TOP, excelente trabajo' y aclara que no requieren intervención con cuestionarios. Si el aula o áreas están mal, advierte diciendo 'Ponle el ojo a esta aula/área, están bajos' y sugiere estrategias paulatinas de mejora.";
        }

        boolean studentAudience = "estudiante".equals(targetAudience);
        var titles = new LinkedHashMap<String, String>();
        if (studentAudience) {
            for (var document : documents) {
                if (document.id() != null && document.courseId() != null) {
                    titles.putIfAbsent("/courses/" + document.courseId() + "/documents/"
                            + document.id() + "/download", document.title());
                }
            }
        }
        String docsContext = titles.entrySet().stream()
                .map(entry -> "Fuente: " + entry.getValue() + " Enlace de descarga: /api/v1" + entry.getKey())
                .collect(Collectors.joining("\n"));
        String referenceInstructions = studentAudience
                ? "Cuando un tema necesite refuerzo, recomienda material pertinente de la lista proporcionada, si existe. "
                    + "Al final cita una sola vez cada documento utilizado: Fuente: <título registrado exacto> "
                    + "Enlace de descarga: <enlace proporcionado>. Copia exactamente título y enlace. "
                    + "No inventes documentos ni reemplaces títulos por encabezados o fragmentos del contenido. "
                    + "No incluyas bloques Fuente con citas textuales ni fuentes dentro de tablas."
                : "Puedes mencionar los temas que requieren repaso, pero NO incluyas enlaces, URLs, "
                    + "citas de fuentes, 'Fuente:', 'Enlace de descarga:' ni referencias al repositorio.";

        String prompt = String.format(
            "Eres un analista académico experto llamado Sery. A continuación te proveo los datos de rendimiento en formato JSON. " +
            "Genera un insight analítico breve y útil (máximo 3 párrafos) dirigido a un %s. " +
            "Destaca los puntos fuertes y los temas donde se necesita refuerzo.\n" +
            "INSTRUCCIONES ESPECÍFICAS SEGÚN TU AUDIENCIA:\n%s\n\n" +
            "%s No cites 'Contexto 1' ni 'Contexto 2'.\n\nDatos de Rendimiento:\n%s\n\n%s",
            targetAudience, audienceInstructions, referenceInstructions, performanceJsonData, docsContext
        );

        GenerateRequest requestBody = new GenerateRequest(
                studentAudience && !docsContext.isBlank()
                        ? List.of("DATOS DE RENDIMIENTO A ANALIZAR: " + performanceJsonData, docsContext)
                        : List.of("DATOS DE RENDIMIENTO A ANALIZAR: " + performanceJsonData),
                List.of(new Message("user", prompt)),
                1000,
                0.7
        );

        HttpEntity<GenerateRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            GenerateResponse response = restTemplate.postForObject(url, entity, GenerateResponse.class);
            if (response == null || response.answer() == null || response.answer().isBlank()) {
                throw new IllegalStateException("AI returned an empty insight.");
            }
            return DocumentReferenceFormatter.format(response.answer(), titles);
        } catch (Exception e) {
            log.warn("AI insight generation request failed.");
            throw new IllegalStateException("Error de conexión con el motor de Inteligencia Artificial.");
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
