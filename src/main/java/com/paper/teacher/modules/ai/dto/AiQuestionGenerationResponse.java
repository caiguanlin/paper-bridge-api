package com.paper.teacher.modules.ai.dto;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.constant.enums.QuestionTypeEnum;

public record AiQuestionGenerationResponse(
        QuestionTypeEnum questionType,
        DifficultyEnum difficulty,
        String stem,
        String contentJson,
        String answerJson,
        String analysis
) {
}
