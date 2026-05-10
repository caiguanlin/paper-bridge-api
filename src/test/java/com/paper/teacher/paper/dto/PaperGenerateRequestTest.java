package com.paper.teacher.paper.dto;

import com.paper.teacher.paper.GenerationStrategy;
import com.paper.teacher.paper.PaperScopeType;
import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;
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
        assertThat(validator().validate(request(PaperScopeType.CHAPTERS, null, List.of())))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapterScopeValid"));
    }

    @Test
    void chapterScopeRejectsBlankChapterEntry() {
        List<PaperGenerateRequest.ChapterScope> chapters = List.of(
                new PaperGenerateRequest.ChapterScope("Unit 1", "Chapter 1"),
                new PaperGenerateRequest.ChapterScope("Unit 1", " ")
        );

        assertThat(validator().validate(request(PaperScopeType.CHAPTERS, null, chapters)))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapters[1].chapter"));
    }

    @Test
    void unitScopeRequiresAtLeastOneUnit() {
        assertThat(validator().validate(request(PaperScopeType.UNITS, List.of(), null)))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("unitScopeValid"));
    }

    @Test
    void volumeScopeDoesNotRequireUnitsOrChapters() {
        assertThat(validator().validate(request(PaperScopeType.VOLUME, null, null))).isEmpty();
    }

    private Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    private PaperGenerateRequest request(
            PaperScopeType scopeType,
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
                GenerationStrategy.BANK_WITH_AI,
                Difficulty.MEDIUM,
                List.of(new PaperGenerateRequest.SectionRequest(
                        "True or False",
                        QuestionType.TRUE_FALSE,
                        1,
                        BigDecimal.TEN
                ))
        );
    }
}
