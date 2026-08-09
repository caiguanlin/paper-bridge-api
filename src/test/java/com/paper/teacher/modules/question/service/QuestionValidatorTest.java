package com.paper.teacher.modules.question.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionValidatorTest {
    private final QuestionValidator validator = new QuestionValidator(new ObjectMapper());

    @Test
    void rejectsMalformedContentJson() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(QuestionTypeEnum.DICTATION, "{not json", "{\"expectedText\":\"床前明月光\"}"));
        assertEquals("题目内容 JSON 不合法", exception.getMessage());
    }

    @Test
    void rejectsNonObjectAnswerJson() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(QuestionTypeEnum.DICTATION, "{\"prompt\":\"默写\"}", "[1,2]"));
        assertEquals("答案 JSON 不合法", exception.getMessage());
    }

    @Test
    void acceptsValidSingleChoice() {
        assertDoesNotThrow(() -> validator.validate(
                QuestionTypeEnum.SINGLE_CHOICE,
                "{\"options\":[\"A. 1\",\"B. 2\"]}",
                "{\"correctOption\":\"A\"}"));
    }

    @Test
    void rejectsSingleChoiceWithTooFewOptions() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.SINGLE_CHOICE,
                "{\"options\":[\"A. 1\"]}",
                "{\"correctOption\":\"A\"}"));
        assertEquals("选择题至少需要两个选项", exception.getMessage());
    }

    @Test
    void rejectsSingleChoiceWithBlankCorrectOption() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.SINGLE_CHOICE,
                "{\"options\":[\"A. 1\",\"B. 2\"]}",
                "{\"correctOption\":\"  \"}"));
        assertEquals("选择题答案必须包含 correctOption", exception.getMessage());
    }

    @Test
    void acceptsValidTrueFalse() {
        assertDoesNotThrow(() -> validator.validate(
                QuestionTypeEnum.TRUE_FALSE,
                "{\"statement\":\"1+1=2\"}",
                "{\"correctBoolean\":true}"));
    }

    @Test
    void rejectsTrueFalseWithNonBooleanAnswer() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.TRUE_FALSE,
                "{\"statement\":\"1+1=2\"}",
                "{\"correctBoolean\":\"true\"}"));
        assertEquals("判断题答案必须包含布尔值 correctBoolean", exception.getMessage());
    }

    @Test
    void acceptsValidFillBlank() {
        assertDoesNotThrow(() -> validator.validate(
                QuestionTypeEnum.FILL_BLANK,
                "{\"blanks\":[\"b1\",\"b2\"]}",
                "{\"acceptedAnswers\":[[\"1\"],[\"2\"]]}"));
    }

    @Test
    void rejectsFillBlankWithoutBlanks() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.FILL_BLANK,
                "{\"blanks\":[]}",
                "{\"acceptedAnswers\":[]}"));
        assertEquals("填空题必须包含 blanks", exception.getMessage());
    }

    @Test
    void rejectsFillBlankWithMismatchedAnswerCount() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.FILL_BLANK,
                "{\"blanks\":[\"b1\",\"b2\"]}",
                "{\"acceptedAnswers\":[[\"1\"]]}"));
        assertEquals("填空题空位数量和答案数量必须一致", exception.getMessage());
    }

    @Test
    void acceptsValidMatching() {
        assertDoesNotThrow(() -> validator.validate(
                QuestionTypeEnum.MATCHING,
                "{\"leftItems\":[\"左1\"],\"rightItems\":[\"右1\"]}",
                "{\"pairs\":[{\"left\":\"左1\",\"right\":\"右1\"}]}"));
    }

    @Test
    void rejectsMatchingWithoutLeftItems() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.MATCHING,
                "{\"rightItems\":[\"右1\"]}",
                "{\"pairs\":[{\"left\":\"左1\",\"right\":\"右1\"}]}"));
        assertEquals("连线题必须包含 leftItems", exception.getMessage());
    }

    @Test
    void rejectsMatchingWithoutRightItems() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.MATCHING,
                "{\"leftItems\":[\"左1\"]}",
                "{\"pairs\":[{\"left\":\"左1\",\"right\":\"右1\"}]}"));
        assertEquals("连线题必须包含 rightItems", exception.getMessage());
    }

    @Test
    void rejectsMatchingWithoutPairs() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.MATCHING,
                "{\"leftItems\":[\"左1\"],\"rightItems\":[\"右1\"]}",
                "{\"pairs\":[]}"));
        assertEquals("连线题答案必须包含 pairs", exception.getMessage());
    }

    @Test
    void acceptsValidDictation() {
        assertDoesNotThrow(() -> validator.validate(
                QuestionTypeEnum.DICTATION,
                "{\"prompt\":\"默写静夜思\"}",
                "{\"expectedText\":\"床前明月光\"}"));
    }

    @Test
    void rejectsDictationWithoutPrompt() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.DICTATION,
                "{}",
                "{\"expectedText\":\"床前明月光\"}"));
        assertEquals("默写题必须包含 prompt", exception.getMessage());
    }

    @Test
    void rejectsDictationWithoutExpectedText() {
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(
                QuestionTypeEnum.DICTATION,
                "{\"prompt\":\"默写静夜思\"}",
                "{}"));
        assertEquals("默写题答案必须包含 expectedText", exception.getMessage());
    }
}
