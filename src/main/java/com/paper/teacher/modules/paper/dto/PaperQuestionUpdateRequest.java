package com.paper.teacher.modules.paper.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaperQuestionUpdateRequest(
        @NotBlank(message = "题干不能为空") String stemSnapshot,
        @NotBlank(message = "题目内容不能为空") String contentSnapshotJson,
        @NotBlank(message = "答案不能为空") String answerSnapshotJson,
        String analysisSnapshot,
        @NotNull(message = "分数不能为空") @DecimalMin(value = "0.01", message = "分数必须大于 0") BigDecimal score
) {
}
