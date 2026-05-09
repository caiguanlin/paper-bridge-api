package com.paper.teacher.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionValidatorTest {
    private final QuestionValidator validator = new QuestionValidator(new ObjectMapper());

    @Test
    void validatesAllSupportedQuestionTypes() {
        validator.validate(QuestionType.SINGLE_CHOICE, "{\"options\":[\"A\",\"B\"]}", "{\"correctOption\":\"A\"}");
        validator.validate(QuestionType.TRUE_FALSE, "{\"statement\":\"1 是数字\"}", "{\"correctBoolean\":true}");
        validator.validate(QuestionType.FILL_BLANK, "{\"blanks\":[\"blank1\"]}", "{\"acceptedAnswers\":[[\"答案\"]]}");
        validator.validate(QuestionType.MATCHING, "{\"leftItems\":[\"一\"],\"rightItems\":[\"1\"]}", "{\"pairs\":[{\"left\":\"一\",\"right\":\"1\"}]}");
        validator.validate(QuestionType.DICTATION, "{\"prompt\":\"默写古诗\"}", "{\"expectedText\":\"床前明月光\"}");
    }

    @Test
    void rejectsChoiceWithoutEnoughOptions() {
        assertThatThrownBy(() -> validator.validate(QuestionType.SINGLE_CHOICE, "{\"options\":[\"A\"]}", "{\"correctOption\":\"A\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("选择题至少需要两个选项");
    }

    @Test
    void rejectsFillBlankAnswerCountMismatch() {
        assertThatThrownBy(() -> validator.validate(QuestionType.FILL_BLANK, "{\"blanks\":[\"a\",\"b\"]}", "{\"acceptedAnswers\":[[\"a\"]]}"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("填空题空位数量和答案数量必须一致");
    }
}
