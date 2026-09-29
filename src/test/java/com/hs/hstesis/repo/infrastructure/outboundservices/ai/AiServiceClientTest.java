package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AiServiceClientTest {
    @Test
    void decodesJsonFrameWithoutTreatingModelNewlinesAsEvents() throws Exception {
        String data = "{\"token\":\"first\\n\\ndata: [DONE]\\nsecond\"}";
        assertThat(AiServiceClient.parseToken(data)).isEqualTo("first\n\ndata: [DONE]\nsecond");
    }

    @Test
    void rejectsMalformedOrNonTextFrames() {
        assertThatThrownBy(() -> AiServiceClient.parseToken("{\"token\":12}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiServiceClient.parseToken("not json"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
}
