package com.paper.teacher.paper.dto;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.constant.enums.GenerationStrategyEnum;
import com.paper.teacher.constant.enums.PaperScopeTypeEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaperGenerateRequestTest {
    @Test
    void chapterScopeRequiresAtLeastOneChapter() {
        assertThat(validator().validate(request(PaperScopeTypeEnum.CHAPTERS, null, List.of())))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapterScopeValid"));
    }

    @Test
    void chapterScopeRejectsBlankChapterEntry() {
        List<PaperGenerateRequest.ChapterScope> chapters = List.of(
                new PaperGenerateRequest.ChapterScope("Unit 1", "Chapter 1"),
                new PaperGenerateRequest.ChapterScope("Unit 1", " ")
        );

        assertThat(validator().validate(request(PaperScopeTypeEnum.CHAPTERS, null, chapters)))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapters[1].chapter"));
    }

    @Test
    void unitScopeRequiresAtLeastOneUnit() {
        assertThat(validator().validate(request(PaperScopeTypeEnum.UNITS, List.of(), null)))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("unitScopeValid"));
    }

    @Test
    void volumeScopeDoesNotRequireUnitsOrChapters() {
        assertThat(validator().validate(request(PaperScopeTypeEnum.VOLUME, null, null))).isEmpty();
    }

    private Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    private PaperGenerateRequest request(
            PaperScopeTypeEnum scopeType,
            List<String> units,
            List<PaperGenerateRequest.ChapterScope> chapters
    ) {
        return new PaperGenerateRequest(
                "Grade 3 Math Quiz",
                "Grade 3",
                "PEP",
                "MATH",
                "Volume 1",
                scopeType,
                units,
                chapters,
                BigDecimal.TEN,
                GenerationStrategyEnum.BANK_WITH_AI,
                DifficultyEnum.MEDIUM,
                List.of(new PaperGenerateRequest.SectionRequest(
                        "True or False",
                        QuestionTypeEnum.TRUE_FALSE,
                        1,
                        BigDecimal.TEN
                ))
        );
    }
}
