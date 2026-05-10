package com.paper.teacher.modules.curriculum.dto;

public record CurriculumSearchRequest(
        String publisher,
        String subject,
        String grade,
        String volume
) {
}
