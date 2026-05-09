package com.paper.teacher.question;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionValidator {
    private final ObjectMapper objectMapper;

    public void validate(QuestionType type, String contentJson, String answerJson) {
        JsonNode content = readObject(contentJson, "题目内容 JSON 不合法");
        JsonNode answer = readObject(answerJson, "答案 JSON 不合法");
        switch (type) {
            case SINGLE_CHOICE -> validateSingleChoice(content, answer);
            case TRUE_FALSE -> validateTrueFalse(answer);
            case FILL_BLANK -> validateFillBlank(content, answer);
            case MATCHING -> validateMatching(content, answer);
            case DICTATION -> validateDictation(content, answer);
        }
    }

    private JsonNode readObject(String json, String error) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new BusinessException(error);
            }
            return node;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(error);
        }
    }

    private void validateSingleChoice(JsonNode content, JsonNode answer) {
        JsonNode options = content.get("options");
        if (options == null || !options.isArray() || options.size() < 2) {
            throw new BusinessException("选择题至少需要两个选项");
        }
        if (!hasText(answer, "correctOption")) {
            throw new BusinessException("选择题答案必须包含 correctOption");
        }
    }

    private void validateTrueFalse(JsonNode answer) {
        JsonNode correct = answer.get("correctBoolean");
        if (correct == null || !correct.isBoolean()) {
            throw new BusinessException("判断题答案必须包含布尔值 correctBoolean");
        }
    }

    private void validateFillBlank(JsonNode content, JsonNode answer) {
        JsonNode blanks = content.get("blanks");
        JsonNode acceptedAnswers = answer.get("acceptedAnswers");
        if (blanks == null || !blanks.isArray() || blanks.isEmpty()) {
            throw new BusinessException("填空题必须包含 blanks");
        }
        if (acceptedAnswers == null || !acceptedAnswers.isArray() || blanks.size() != acceptedAnswers.size()) {
            throw new BusinessException("填空题空位数量和答案数量必须一致");
        }
    }

    private void validateMatching(JsonNode content, JsonNode answer) {
        JsonNode leftItems = content.get("leftItems");
        JsonNode rightItems = content.get("rightItems");
        JsonNode pairs = answer.get("pairs");
        if (leftItems == null || !leftItems.isArray() || leftItems.isEmpty()) {
            throw new BusinessException("连线题必须包含 leftItems");
        }
        if (rightItems == null || !rightItems.isArray() || rightItems.isEmpty()) {
            throw new BusinessException("连线题必须包含 rightItems");
        }
        if (pairs == null || !pairs.isArray() || pairs.isEmpty()) {
            throw new BusinessException("连线题答案必须包含 pairs");
        }
    }

    private void validateDictation(JsonNode content, JsonNode answer) {
        if (!hasText(content, "prompt")) {
            throw new BusinessException("默写题必须包含 prompt");
        }
        if (!hasText(answer, "expectedText")) {
            throw new BusinessException("默写题答案必须包含 expectedText");
        }
    }

    private boolean hasText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && StrUtil.isNotBlank(value.asText());
    }
}
