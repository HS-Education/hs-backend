package com.hs.hstesis.repo.infrastructure.outboundservices.ai;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.StringReader;

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

    @Test
    void streamEofWithoutDoneIsReportedAsFailure() {
        var tokens = new ArrayList<String>();
        var reader = new BufferedReader(new StringReader("data: {\"token\":\"partial\"}\n\n"));

        assertThatThrownBy(() -> AiServiceClient.consumeStream(reader, tokens::add))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("without a completion signal");
        assertThat(tokens).containsExactly("partial");
    }

    @Test
    void streamWithDoneIsAcceptedAsComplete() throws Exception {
        var tokens = new ArrayList<String>();
        var reader = new BufferedReader(new StringReader(
                "data: {\"token\":\"answer\"}\n\ndata: [DONE]\n\n"));

        AiServiceClient.consumeStream(reader, tokens::add);

        assertThat(tokens).containsExactly("answer");
    }
}
