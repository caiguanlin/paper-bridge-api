package com.paper.teacher.modules.question.dto;

import java.util.List;

public record QuestionImportResult(int successCount, int failureCount, List<RowError> errors) {
    public record RowError(int rowNumber, String fieldName, String message) {
    }
}
