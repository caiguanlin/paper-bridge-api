package com.paper.teacher.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;
import com.paper.teacher.question.QuestionValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiQuestionValidatorTest {
    private final AiQuestionValidator validator = new AiQuestionValidator(new QuestionValidator(new ObjectMapper()));

    @Test
    void acceptsValidAiQuestion() {
        validator.validate(new AiQuestionGenerationResponse(
                QuestionType.TRUE_FALSE,
                Difficulty.EASY,
                "1 是数字",
                "{\"statement\":\"1 是数字\"}",
                "{\"correctBoolean\":true}",
                "示例解析"
        ));
    }

    @Test
    void rejectsInvalidAiQuestion() {
        assertThatThrownBy(() -> validator.validate(new AiQuestionGenerationResponse(
                QuestionType.TRUE_FALSE,
                Difficulty.EASY,
                "1 是数字",
                "{\"statement\":\"1 是数字\"}",
                "{\"correctBoolean\":\"是\"}",
                "示例解析"
        ))).isInstanceOf(BusinessException.class);
    }
}
