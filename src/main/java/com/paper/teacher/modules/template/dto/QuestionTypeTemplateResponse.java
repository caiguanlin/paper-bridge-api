package com.paper.teacher.modules.template.dto;

import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.template.entity.QuestionTypeTemplate;
import com.paper.teacher.modules.template.entity.QuestionTypeTemplateItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionTypeTemplateResponse(
        Long id,
        String name,
        BigDecimal totalScore,
        Integer sortOrder,
        LocalDateTime updatedAt,
        List<ItemResponse> items
) {
    public static QuestionTypeTemplateResponse from(
            QuestionTypeTemplate template,
            List<QuestionTypeTemplateItem> items
    ) {
        return new QuestionTypeTemplateResponse(
                template.getId(),
                template.getName(),
                template.getTotalScore(),
                template.getSortOrder(),
                template.getUpdatedAt(),
                items.stream().map(ItemResponse::from).toList()
        );
    }

    public record ItemResponse(
            Long id,
            String title,
            QuestionTypeEnum questionType,
            Integer questionCount,
            BigDecimal scorePerQuestion,
            Integer sortOrder
    ) {
        private static ItemResponse from(QuestionTypeTemplateItem item) {
            return new ItemResponse(
                    item.getId(),
                    item.getTitle(),
                    item.getQuestionType(),
                    item.getQuestionCount(),
                    item.getScorePerQuestion(),
                    item.getSortOrder()
            );
        }
    }
}
