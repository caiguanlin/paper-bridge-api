package com.paper.teacher.paper.dto;

import com.paper.teacher.paper.GenerationStrategy;
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
    void requiresAtLeastOneChapter() {
        assertThat(validator().validate(request(List.of())))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapters"));
    }

    @Test
    void rejectsBlankChapterEntry() {
        assertThat(validator().validate(request(List.of("Measurement", " "))))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chapters[1].<list element>"));
    }

    private Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    private PaperGenerateRequest request(List<String> chapters) {
        return new PaperGenerateRequest(
                "Grade 3 Math Quiz",
                "Grade 3",
                "PEP",
                "MATH",
                "Volume 1",
                "Unit 3",
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
