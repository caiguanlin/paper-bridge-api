package com.paper.teacher.question.dto;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;

public record QuestionSearchRequest(
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        QuestionType questionType,
        Difficulty difficulty
) {
}
