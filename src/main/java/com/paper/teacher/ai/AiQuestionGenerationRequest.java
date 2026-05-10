package com.paper.teacher.ai;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;

import java.math.BigDecimal;
import java.util.List;

public record AiQuestionGenerationRequest(
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        List<String> chapters,
        QuestionType questionType,
        Difficulty difficulty,
        int count,
        BigDecimal scorePerQuestion
) {
}
