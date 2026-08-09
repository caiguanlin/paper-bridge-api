package com.paper.teacher.modules.question.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionValidatorTest {
    private final QuestionValidator validator = new QuestionValidator(new ObjectMapper());

    @Test
    void malformedJsonKeepsParserDetailAndCause() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.TRUE_FALSE, "{\"statement\":", "{\"correctBoolean\":true}"));

        assertTrue(exception.getMessage().startsWith("题目内容 JSON 不合法："));
        assertTrue(exception.getMessage().length() > "题目内容 JSON 不合法：".length());
        assertTrue(exception.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException);
    }

    @Test
    void blankJsonIsRejectedWithReason() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.TRUE_FALSE, "  ", "{\"correctBoolean\":true}"));

        assertEquals("题目内容 JSON 不合法：内容为空", exception.getMessage());
    }

    @Test
    void jsonArrayIsRejectedWithReason() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.TRUE_FALSE, "[]", "{\"correctBoolean\":true}"));

        assertEquals("题目内容 JSON 不合法：必须是 JSON 对象", exception.getMessage());
    }

    @Test
    void missingTypeIsRejected() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(null, "{}", "{}"));

        assertEquals("题型不能为空", exception.getMessage());
    }

    @Test
    void validQuestionPasses() {
        validator.validate(QuestionTypeEnum.TRUE_FALSE, "{\"statement\":\"1+1=2\"}", "{\"correctBoolean\":true}");
    }
}
