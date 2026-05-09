package com.paper.teacher.paper.dto;

import com.paper.teacher.paper.Paper;
import com.paper.teacher.paper.PaperQuestion;
import com.paper.teacher.paper.PaperSection;
import com.paper.teacher.paper.PaperStatus;
import com.paper.teacher.question.QuestionSource;
import com.paper.teacher.question.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaperResponse(
        Long id,
        String title,
        String grade,
        String publisher,
        String subject,
        String volume,
        String unit,
        String chapter,
        BigDecimal totalScore,
        PaperStatus status,
        LocalDateTime updatedAt,
        List<SectionResponse> sections
) {
    public static PaperResponse from(Paper paper, List<SectionResponse> sections) {
        return new PaperResponse(
                paper.getId(), paper.getTitle(), paper.getGrade(), paper.getPublisher(), paper.getSubject(),
                paper.getVolume(), paper.getUnit(), paper.getChapter(), paper.getTotalScore(), paper.getStatus(),
                paper.getUpdatedAt(), sections
        );
    }

    public record SectionResponse(
            Long id,
            String title,
            QuestionType questionType,
            int questionCount,
            BigDecimal scorePerQuestion,
            BigDecimal subtotalScore,
            int sortOrder,
            List<QuestionResponse> questions
    ) {
        public static SectionResponse from(PaperSection section, List<QuestionResponse> questions) {
            return new SectionResponse(
                    section.getId(), section.getTitle(), section.getQuestionType(), section.getQuestionCount(),
                    section.getScorePerQuestion(), section.getSubtotalScore(), section.getSortOrder(), questions
            );
        }
    }

    public record QuestionResponse(
            Long id,
            Long sourceQuestionId,
            QuestionSource source,
            String stemSnapshot,
            String contentSnapshotJson,
            String answerSnapshotJson,
            String analysisSnapshot,
            BigDecimal score,
            int sortOrder
    ) {
        public static QuestionResponse from(PaperQuestion question) {
            return new QuestionResponse(
                    question.getId(), question.getSourceQuestionId(), question.getSource(), question.getStemSnapshot(),
                    question.getContentSnapshotJson(), question.getAnswerSnapshotJson(), question.getAnalysisSnapshot(),
                    question.getScore(), question.getSortOrder()
            );
        }
    }
}
