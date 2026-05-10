package com.paper.teacher.modules.curriculum.dto;

import jakarta.validation.constraints.NotBlank;

public record CurriculumUpsertRequest(
        @NotBlank(message = "出版社不能为空") String publisher,
        @NotBlank(message = "科目不能为空") String subject,
        @NotBlank(message = "年级不能为空") String grade,
        @NotBlank(message = "册别不能为空") String volume,
        @NotBlank(message = "单元不能为空") String unit,
        @NotBlank(message = "章节标题不能为空") String chapter,
        Integer sortOrder,
        Integer editionYear,
        String sourceUrl
) {
}
