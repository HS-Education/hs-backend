package com.hs.hstesis.achievements.application.internal.outboundservices.ai;

import com.hs.hstesis.repo.interfaces.acl.dto.DocumentBasicData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExternalAiServiceTest {
    private final RestTemplate rest = mock(RestTemplate.class);
    private final ExternalAiService service = new ExternalAiService(rest);
    private final List<DocumentBasicData> documents = List.of(new DocumentBasicData(30L, "30pg", 10L));
    private static final String LINK = "/api/v1/courses/10/documents/30/download";

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "aiServiceUrl", "http://ai.test");
        ReflectionTestUtils.setField(service, "apiKey", "");
    }

    private void response(String answer) {
        when(rest.postForObject(anyString(), any(), eq(ExternalAiService.GenerateResponse.class)))
                .thenReturn(new ExternalAiService.GenerateResponse("fake", answer, 1, 1));
    }

    private ExternalAiService.GenerateRequest request() {
        var captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(eq("http://ai.test/generate"), captor.capture(), eq(ExternalAiService.GenerateResponse.class));
        return (ExternalAiService.GenerateRequest) captor.getValue().getBody();
    }

    @Test void studentKeepsOnlyRegisteredTitlesAndOneReferencePerDocument() {
        response("Repasa probabilidad.\n**Fuente**: “fragmento citado sin documento”\n"
                + "Fuente: Invented title Enlace de descarga: " + LINK
                + "\nFuente: another name Enlace de descarga: " + LINK);
        String answer = service.generateInsight("{\"score\":10}", "estudiante", "ST-42", documents);
        assertThat(answer).contains("Repasa probabilidad.", "Fuente: 30pg Enlace de descarga: " + LINK)
                .doesNotContain("Invented title", "another name", "fragmento citado");
        assertThat(answer.indexOf("Fuente: 30pg")).isEqualTo(answer.lastIndexOf("Fuente: 30pg"));
        var sent = request();
        assertThat(String.join(" ", sent.context_chunks())).contains("Fuente: 30pg", LINK);
        assertThat(sent.messages().getFirst().content()).contains("título registrado exacto", "ST-42");
    }

    @Test void teacherHasNoReferencesEvenIfModelAddsThem() {
        response("Refuerza el tema.\nFuente: invented Enlace de descarga: " + LINK);
        assertThat(service.generateInsight("{}", "profesor", null, documents))
                .contains("Refuerza el tema.").doesNotContain("Fuente:", LINK, "invented");
        assertThat(String.join(" ", request().context_chunks())).doesNotContain("30pg", LINK);
    }

    @Test void coordinatorHasNoRepositoryMarkdownLinksEvenIfModelAddsThem() {
        response("Revisa el rendimiento. Material de apoyo: [invented](" + LINK + ")");
        assertThat(service.generateInsight("{}", "coordinador académico", null, documents))
                .contains("Revisa el rendimiento.").doesNotContain("invented", LINK, "Fuente:");
        assertThat(String.join(" ", request().context_chunks())).doesNotContain("30pg", LINK);
    }

    @Test void studentCannotReceiveInventedReferencesWhenNoDocumentsAreAvailable() {
        response("Repasa el tema.\nFuente: invented Enlace de descarga: " + LINK);
        assertThat(service.generateInsight("{}", "estudiante", "ST-42", List.of()))
                .contains("Repasa el tema.").doesNotContain("Fuente:", LINK, "invented");
    }
}
