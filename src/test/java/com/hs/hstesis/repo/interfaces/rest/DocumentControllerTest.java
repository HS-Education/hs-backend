package com.hs.hstesis.repo.interfaces.rest;

import com.hs.hstesis.repo.domain.services.DocumentCommandService;
import com.hs.hstesis.repo.domain.services.DocumentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentControllerTest {
    @Test
    void malformedMetadataDoesNotEchoUntrustedBody() {
        var commands = mock(DocumentCommandService.class);
        var queries = mock(DocumentQueryService.class);
        var controller = new DocumentController(commands, queries);
        var file = new MockMultipartFile("files", "lesson.pdf", "application/pdf", new byte[0]);
        String marker = "PRIVATE_PDF_CONTENT_DO_NOT_ECHO";

        assertThatThrownBy(() -> controller.uploadBulkDocuments(1L, List.of(file), "{\"password\":\"" + marker + "\""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JSON format in 'data' field.")
                .hasMessageNotContaining(marker);
        verifyNoInteractions(commands, queries);
    }
}
