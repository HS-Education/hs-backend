package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

@Service
public class AiServiceClient {

    private final RestClient restClient;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String baseUrl,
            @Value("${ai.service.api-key:your-secure-api-key}") String apiKey) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(45));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    public EmbedQueryResponse embedQuery(String text) {
        return restClient.post()
                .uri("/embed-query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedQueryRequest(text))
                .retrieve()
                .body(EmbedQueryResponse.class);
    }

    public GenerateResponse generateAnswer(GenerateRequest request) {
        return restClient.post()
                .uri("/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);
    }

    public void generateAnswerStream(GenerateRequest request, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            restClient.post()
                    .uri("/generate-stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            throw new IOException("AI stream returned HTTP " + res.getStatusCode().value());
                        }
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }
                                    onToken.accept(parseToken(data));
                                }
                            }
                        }
                        onComplete.run();
                        return null;
                    });
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    static String parseToken(String data) throws JsonProcessingException {
        var node = MAPPER.readTree(data);
        if (!node.isObject() || !node.hasNonNull("token") || !node.get("token").isTextual()) {
            throw new IllegalArgumentException("Invalid AI stream event");
        }
        return node.get("token").asText();
    }
}
