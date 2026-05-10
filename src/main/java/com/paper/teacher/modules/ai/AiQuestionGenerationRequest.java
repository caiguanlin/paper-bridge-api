package com.paper.teacher.modules.ai;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.constant.enums.QuestionTypeEnum;

import java.math.BigDecimal;

public record AiQuestionGenerationRequest(
        String grade,
        String publisher,
        String subject,
        String volume,
        String scopeDescription,
        QuestionTypeEnum questionType,
        DifficultyEnum difficulty,
        int count,
        BigDecimal scorePerQuestion
) {
}
