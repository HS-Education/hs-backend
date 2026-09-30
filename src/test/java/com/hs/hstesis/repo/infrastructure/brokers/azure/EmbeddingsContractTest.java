package com.hs.hstesis.repo.infrastructure.brokers.azure;

import com.hs.hstesis.repo.infrastructure.brokers.rabbitmq.dtos.ChunkVector;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class EmbeddingsContractTest {
    private final String hash = "a".repeat(64);
    private EmbeddingsReference reference() { return new EmbeddingsReference(1, 2, 3,
            "processing-results/2/3/" + hash + ".json", hash, 4000, 1, 1024); }
    @Test void validReferenceAndPayloadMatch() {
        var payload = new EmbeddingsPayload(2, 3, List.of(new ChunkVector(1, 0, "lesson", new float[1024])));
        assertThatCode(() -> payload.validateAgainst(reference())).doesNotThrowAnyException();
    }
    @Test void arbitraryUrlsAndPathsAreRejected() {
        for (String key : List.of("https://evil.invalid/result", "processing-results/../secrets", "documents/a.pdf")) {
            assertThatThrownBy(() -> new EmbeddingsReference(1, 2, 3, key, hash, 100, 1, 1024))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void generationsCannotCross() {
        assertThatThrownBy(() -> new EmbeddingsPayload(2, 2,
                List.of(new ChunkVector(1, 0, "lesson", new float[1024]))).validateAgainst(reference()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void wrongVectorDimensionsAndNonfiniteNumbersAreRejected() {
        float[] nonfinite = new float[1024]; nonfinite[0] = Float.NaN;
        for (float[] vector : List.of(new float[768], nonfinite)) {
            assertThatThrownBy(() -> new EmbeddingsPayload(2, 3,
                    List.of(new ChunkVector(1, 0, "lesson", vector))).validateAgainst(reference()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void oversizedReferenceIsRejected() {
        assertThatThrownBy(() -> new EmbeddingsReference(1, 2, 3, reference().objectKey(), hash,
                64L * 1024 * 1024 + 1, 1, 1024)).isInstanceOf(IllegalArgumentException.class);
    }
}
