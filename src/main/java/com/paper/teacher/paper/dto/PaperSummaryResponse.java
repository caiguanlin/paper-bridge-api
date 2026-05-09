package com.paper.teacher.paper.dto;

import com.paper.teacher.paper.Paper;
import com.paper.teacher.paper.PaperStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaperSummaryResponse(
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
        LocalDateTime updatedAt
) {
    public static PaperSummaryResponse from(Paper paper) {
        return new PaperSummaryResponse(
                paper.getId(), paper.getTitle(), paper.getGrade(), paper.getPublisher(), paper.getSubject(),
                paper.getVolume(), paper.getUnit(), paper.getChapter(), paper.getTotalScore(), paper.getStatus(),
                paper.getUpdatedAt()
        );
    }
}
