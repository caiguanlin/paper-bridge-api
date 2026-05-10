package com.paper.teacher.modules.question.dto;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.constant.enums.QuestionTypeEnum;

public record QuestionSearchRequest(
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        QuestionTypeEnum questionType,
        DifficultyEnum difficulty
) {
}
