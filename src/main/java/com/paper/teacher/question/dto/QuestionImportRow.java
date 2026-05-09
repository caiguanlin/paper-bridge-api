package com.paper.teacher.question.dto;

public record QuestionImportRow(
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        String questionType,
        String difficulty,
        String stem,
        String contentJson,
        String answerJson,
        String analysis
) {
}
