package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

@Service
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(
            @Value("${ai.worker.url:http://localhost:8000}") String baseUrl,
            @Value("${ai.worker.api-key:your-secure-api-key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getBody()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String token = line.substring(6);
                                    if ("[DONE]".equals(token)) {
                                        break;
                                    }
                                    onToken.accept(token);
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
}
