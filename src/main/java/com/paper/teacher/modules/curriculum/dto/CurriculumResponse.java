package com.paper.teacher.modules.curriculum.dto;

import com.paper.teacher.modules.curriculum.CurriculumNode;

public record CurriculumResponse(
        Long id,
        String publisher,
        String subject,
        String grade,
        String volume,
        String unit,
        String chapter,
        Integer sortOrder,
        Integer editionYear,
        String sourceUrl
) {
    public static CurriculumResponse from(CurriculumNode node) {
        return new CurriculumResponse(
                node.getId(),
                node.getPublisher(),
                node.getSubject(),
                node.getGrade(),
                node.getVolume(),
                node.getUnit(),
                node.getChapter(),
                node.getSortOrder(),
                node.getEditionYear(),
                node.getSourceUrl()
        );
    }
}
