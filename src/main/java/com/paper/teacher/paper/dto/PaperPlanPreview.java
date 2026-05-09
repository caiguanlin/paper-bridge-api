package com.paper.teacher.paper.dto;

import com.paper.teacher.question.QuestionType;

import java.math.BigDecimal;
import java.util.List;

public record PaperPlanPreview(BigDecimal totalScore, BigDecimal subtotalScore, List<SectionPreview> sections) {
    public record SectionPreview(
            String title,
            QuestionType questionType,
            int requiredCount,
            int availableBankCount,
            int aiSupplementCount,
            BigDecimal subtotalScore
    ) {
    }
}
