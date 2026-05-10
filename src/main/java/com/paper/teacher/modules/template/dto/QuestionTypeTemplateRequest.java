package com.paper.teacher.modules.template.dto;

import com.paper.teacher.constant.enums.QuestionTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record QuestionTypeTemplateRequest(
        @NotBlank(message = "模板名称不能为空") String name,
        @NotEmpty(message = "至少需要一个题型配置") List<@Valid ItemRequest> items
) {
    public record ItemRequest(
            @NotBlank(message = "题型标题不能为空") String title,
            @NotNull(message = "题型不能为空") QuestionTypeEnum questionType,
            @Positive(message = "题目数量必须大于 0") int questionCount,
            @NotNull(message = "每题分数不能为空") @DecimalMin(value = "0.01", message = "每题分数必须大于 0") BigDecimal scorePerQuestion
    ) {
        public BigDecimal subtotal() {
            return scorePerQuestion.multiply(BigDecimal.valueOf(questionCount));
        }
    }
}
