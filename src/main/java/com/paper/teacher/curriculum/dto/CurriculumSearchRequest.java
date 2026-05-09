package com.paper.teacher.curriculum.dto;

public record CurriculumSearchRequest(
        String publisher,
        String subject,
        String grade,
        String volume
) {
}
