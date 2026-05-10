package com.paper.teacher.ai;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.ai.AiQuestionGenerationResponse;
import com.paper.teacher.modules.ai.AiQuestionValidator;
import com.paper.teacher.modules.question.QuestionValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiQuestionValidatorTest {
    private final AiQuestionValidator validator = new AiQuestionValidator(new QuestionValidator(new ObjectMapper()));

    @Test
    void acceptsValidAiQuestion() {
        validator.validate(new AiQuestionGenerationResponse(
                QuestionTypeEnum.TRUE_FALSE,
                DifficultyEnum.EASY,
                "1 是数字",
                "{\"statement\":\"1 是数字\"}",
                "{\"correctBoolean\":true}",
                "示例解析"
        ));
    }

    @Test
    void rejectsInvalidAiQuestion() {
        assertThatThrownBy(() -> validator.validate(new AiQuestionGenerationResponse(
                QuestionTypeEnum.TRUE_FALSE,
                DifficultyEnum.EASY,
                "1 是数字",
                "{\"statement\":\"1 是数字\"}",
                "{\"correctBoolean\":\"是\"}",
                "示例解析"
        ))).isInstanceOf(BusinessException.class);
    }
}
