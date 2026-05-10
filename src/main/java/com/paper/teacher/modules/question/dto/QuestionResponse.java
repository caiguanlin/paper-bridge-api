package com.paper.teacher.modules.question.dto;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.modules.question.Question;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long id,
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        QuestionTypeEnum questionType,
        DifficultyEnum difficulty,
        String stem,
        String contentJson,
        String answerJson,
        String analysis,
        QuestionSourceEnum source,
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
