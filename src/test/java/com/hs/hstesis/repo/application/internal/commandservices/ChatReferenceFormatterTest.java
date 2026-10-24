package com.hs.hstesis.repo.application.internal.commandservices;

import com.hs.hstesis.repo.infrastructure.persistance.jpa.repositories.DocumentChunkWithMetadata;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatReferenceFormatterTest {
    private static final String LINK = "/api/v1/courses/10/documents/30/download";
    private static final String REFERENCE = "Fuente: 30pg Enlace de descarga: " + LINK;

    private DocumentChunkWithMetadata source() {
        var source = mock(DocumentChunkWithMetadata.class);
        when(source.getTitle()).thenReturn("30pg");
        when(source.getCourseId()).thenReturn(10L);
        when(source.getDocumentId()).thenReturn(30L);
        return source;
    }

    @Test void restoresRegisteredTitleAndDeduplicatesModelReferences() {
        var answer = "Academic explanation.\nFuente: Probability lesson Enlace de descarga: " + LINK
                + "\nFuente: lanzamiento de un dado Enlace de descarga: " + LINK;
        String result = ChatReferenceFormatter.format(answer, List.of(source()));
        assertThat(result).contains("Academic explanation.", REFERENCE)
                .doesNotContain("Probability lesson", "lanzamiento de un dado");
        assertThat(result.indexOf(REFERENCE)).isEqualTo(result.lastIndexOf(REFERENCE));
    }

    @Test void stripsQuotedSourceBlockAndRejectsUnretrievedDocument() {
        String result = ChatReferenceFormatter.format(
                "Answer.\n**Fuente**: “Ejercicios: 500 x 0.10 = 50”【 &#x20;\n"
                + "Fuente: Fake Enlace de descarga: /api/v1/courses/99/documents/99/download",
                List.of(source()));
        assertThat(result).contains("Answer.").doesNotContain("Ejercicios", "50", "Fake", "download", "&#x20;");
    }

    @Test void streamedAndNonStreamedResultsMatchEvenWhenSourceLabelsSplitAcrossTokens() {
        var chunk = source();
        String answer = "Answer.\n**Fuente:** invented title\n**Enlace de descarga:** " + LINK;
        var formatter = new ChatReferenceFormatter(List.of(chunk));
        var streamed = new StringBuilder();
        for (int i = 0; i < answer.length(); i++) {
            String emitted = formatter.accept(answer.substring(i, i + 1));
            assertThat(emitted).doesNotContain("invented title");
            streamed.append(emitted);
        }
        streamed.append(formatter.finish());
        assertThat(streamed.toString()).isEqualTo(ChatReferenceFormatter.format(answer, List.of(chunk)))
                .contains(REFERENCE).doesNotContain("invented title");
    }

    @Test void preservesOrdinaryAnswerWithoutFabricatingReferences() {
        assertThat(ChatReferenceFormatter.format("Answer without citations.", List.of(source())))
                .isEqualTo("Answer without citations.");
    }
    @Test void removesDanglingCitationMarksWithoutChangingExplanationOrFinalReference() {
        String answer = "Los fenómenos deterministas permiten predecir el resultado【\n"
                + "*Ejemplo*: d = v·t.\n"
                + "El espacio muestral es W = {1, 2, 3, 4, 5, 6}【\n"
                + "Fuente: wrong title Enlace de descarga: " + LINK;
        String result = ChatReferenceFormatter.format(answer, List.of(source()));
        assertThat(result).contains("Los fenómenos deterministas permiten predecir el resultado",
                        "*Ejemplo*: d = v·t.", "W = {1, 2, 3, 4, 5, 6}", REFERENCE)
                .doesNotContain("【", "】", "wrong title");
        assertThat(result.indexOf(REFERENCE)).isEqualTo(result.lastIndexOf(REFERENCE));
    }

    @Test void removesInlineQuotedCitationButKeepsTextFollowingItAndRegisteredTitle() {
        String answer = "Explicación【Fuente: fragmento literal Enlace de descarga: " + LINK
                + "】. El ejemplo continúa.\nFuente: duplicate Enlace de descarga: " + LINK;
        String result = ChatReferenceFormatter.format(answer, List.of(source()));
        assertThat(result).contains("Explicación. El ejemplo continúa.", REFERENCE)
                .doesNotContain("fragmento literal", "【", "】", "duplicate");
        assertThat(result.indexOf(REFERENCE)).isEqualTo(result.lastIndexOf(REFERENCE));
    }

    @Test void citationCleanupAlsoWorksAcrossIndividualStreamTokens() {
        var chunk = source();
        String answer = "Explicación【Fuente: fragmento Enlace de descarga: " + LINK + "】. Sigue.\n";
        var formatter = new ChatReferenceFormatter(List.of(chunk));
        var emitted = new StringBuilder();
        for (int i = 0; i < answer.length(); i++) {
            String text = formatter.accept(answer.substring(i, i + 1));
            assertThat(text).doesNotContain("【", "】", "fragmento");
            emitted.append(text);
        }
        emitted.append(formatter.finish());
        assertThat(emitted.toString()).isEqualTo(ChatReferenceFormatter.format(answer, List.of(chunk)))
                .contains("Explicación. Sigue.", REFERENCE);
    }

}
