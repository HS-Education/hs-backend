package com.hs.hstesis.assessments.interfaces.rest.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.hstesis.assessments.domain.model.entities.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionResourceFromEntityAssemblerTest {
    @Test
    void studentQuestionDtoDoesNotExposeTheCorrectAnswer() throws Exception {
        Question question = new Question();
        question.setId(1L);
        question.setTopicId(2L);
        question.setText("¿Cuál es el resultado?");
        question.setOptions(List.of("3", "4", "5"));
        question.setCorrectOptionIndex(2);
        question.setIsRemedial(false);

        var dto = QuestionResourceFromEntityAssembler.toResourceFromEntity(question);
        var json = new ObjectMapper().writeValueAsString(dto);

        assertThat(json).contains("¿Cuál es el resultado?");
        assertThat(json).doesNotContain("correctOptionIndex", "correctAnswer");
        assertThat(dto.options()).containsExactly("3", "4", "5");
    }
}
