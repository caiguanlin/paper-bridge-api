package com.paper.teacher.question.dto;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.Question;
import com.paper.teacher.question.QuestionSource;
import com.paper.teacher.question.QuestionType;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long id,
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        QuestionType questionType,
        Difficulty difficulty,
        String stem,
        String contentJson,
        String answerJson,
        String analysis,
        QuestionSource source,
        Integer usageCount,
        LocalDateTime updatedAt
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getGrade(),
                question.getPublisher(),
                question.getSubject(),
                question.getVolume(),
                question.getUnit(),
                question.getChapter(),
                question.getQuestionType(),
                question.getDifficulty(),
                question.getStem(),
                question.getContentJson(),
                question.getAnswerJson(),
                question.getAnalysis(),
                question.getSource(),
                question.getUsageCount(),
                question.getUpdatedAt()
        );
    }
}
