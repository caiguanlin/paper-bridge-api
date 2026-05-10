package com.paper.teacher.modules.paper.dto;

import com.paper.teacher.constant.enums.QuestionTypeEnum;

import java.math.BigDecimal;
import java.util.List;

public record PaperPlanPreview(BigDecimal totalScore, BigDecimal subtotalScore, List<SectionPreview> sections) {
    public record SectionPreview(
            String title,
            QuestionTypeEnum questionType,
            int requiredCount,
            int availableBankCount,
            int aiSupplementCount,
            BigDecimal subtotalScore
    ) {
    }
}
