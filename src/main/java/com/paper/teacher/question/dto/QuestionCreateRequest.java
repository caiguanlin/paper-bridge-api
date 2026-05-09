package com.paper.teacher.question.dto;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionCreateRequest(
        @NotBlank(message = "年级不能为空") String grade,
        @NotBlank(message = "出版社不能为空") String publisher,
        @NotBlank(message = "科目不能为空") String subject,
        @NotBlank(message = "册别不能为空") String volume,
        @NotBlank(message = "单元不能为空") String unit,
        @NotBlank(message = "章节不能为空") String chapter,
        @NotNull(message = "题型不能为空") QuestionType questionType,
        @NotNull(message = "难度不能为空") Difficulty difficulty,
        @NotBlank(message = "题干不能为空") String stem,
        @NotBlank(message = "题目内容不能为空") String contentJson,
        @NotBlank(message = "答案不能为空") String answerJson,
        String analysis
) {
}
