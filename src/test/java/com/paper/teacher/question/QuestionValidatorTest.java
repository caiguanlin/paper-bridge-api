package com.paper.teacher.question;

import com.paper.teacher.constant.enums.QuestionTypeEnum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.modules.question.QuestionValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionValidatorTest {
    private final QuestionValidator validator = new QuestionValidator(new ObjectMapper());

    @Test
    void validatesAllSupportedQuestionTypes() {
        validator.validate(QuestionTypeEnum.SINGLE_CHOICE, "{\"options\":[\"A\",\"B\"]}", "{\"correctOption\":\"A\"}");
        validator.validate(QuestionTypeEnum.TRUE_FALSE, "{\"statement\":\"1 是数字\"}", "{\"correctBoolean\":true}");
        validator.validate(QuestionTypeEnum.FILL_BLANK, "{\"blanks\":[\"blank1\"]}", "{\"acceptedAnswers\":[[\"答案\"]]}");
        validator.validate(QuestionTypeEnum.MATCHING, "{\"leftItems\":[\"一\"],\"rightItems\":[\"1\"]}", "{\"pairs\":[{\"left\":\"一\",\"right\":\"1\"}]}");
        validator.validate(QuestionTypeEnum.DICTATION, "{\"prompt\":\"默写古诗\"}", "{\"expectedText\":\"床前明月光\"}");
    }

    @Test
    void rejectsChoiceWithoutEnoughOptions() {
        assertThatThrownBy(() -> validator.validate(QuestionTypeEnum.SINGLE_CHOICE, "{\"options\":[\"A\"]}", "{\"correctOption\":\"A\"}"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("选择题至少需要两个选项");
    }

    @Test
    void rejectsFillBlankAnswerCountMismatch() {
        assertThatThrownBy(() -> validator.validate(QuestionTypeEnum.FILL_BLANK, "{\"blanks\":[\"a\",\"b\"]}", "{\"acceptedAnswers\":[[\"a\"]]}"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("填空题空位数量和答案数量必须一致");
    }
}
