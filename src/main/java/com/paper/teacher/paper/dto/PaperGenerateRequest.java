package com.paper.teacher.paper.dto;

import com.paper.teacher.paper.GenerationStrategy;
import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record PaperGenerateRequest(
        @NotBlank(message = "试卷标题不能为空") String title,
        @NotBlank(message = "年级不能为空") String grade,
        @NotBlank(message = "出版社不能为空") String publisher,
        @NotBlank(message = "科目不能为空") String subject,
        @NotBlank(message = "册别不能为空") String volume,
        @NotBlank(message = "单元不能为空") String unit,
        @NotEmpty(message = "至少需要一个章节") List<@NotBlank(message = "章节不能为空") String> chapters,
        @NotNull(message = "总分不能为空") @DecimalMin(value = "0.01", message = "总分必须大于 0") BigDecimal totalScore,
        @NotNull(message = "生成策略不能为空") GenerationStrategy strategy,
        Difficulty difficulty,
        @NotEmpty(message = "至少需要一个题型区块") List<@Valid SectionRequest> sections
) {
    public record SectionRequest(
            @NotBlank(message = "题型区块标题不能为空") String title,
            @NotNull(message = "题型不能为空") QuestionType questionType,
            @Positive(message = "题目数量必须大于 0") int questionCount,
            @NotNull(message = "每题分数不能为空") @DecimalMin(value = "0.01", message = "每题分数必须大于 0") BigDecimal scorePerQuestion
    ) {
        public BigDecimal subtotal() {
            return scorePerQuestion.multiply(BigDecimal.valueOf(questionCount));
        }
    }
}
