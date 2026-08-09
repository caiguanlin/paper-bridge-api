package com.paper.teacher.modules.ai.service;

import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;
import com.paper.teacher.modules.question.service.QuestionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiQuestionValidatorTest {
    @Mock
    private QuestionValidator questionValidator;
    @InjectMocks
    private AiQuestionValidator aiQuestionValidator;

    @Test
    void delegatesToQuestionValidatorWithSnapshotPayloads() {
        AiQuestionGenerationResponse response = new AiQuestionGenerationResponse(
                QuestionTypeEnum.TRUE_FALSE, DifficultyEnum.EASY, "1+1=2",
                "{\"statement\":\"1+1=2\"}", "{\"correctBoolean\":true}", "基础运算");

        aiQuestionValidator.validate(response);

        verify(questionValidator).validate(
                QuestionTypeEnum.TRUE_FALSE, "{\"statement\":\"1+1=2\"}", "{\"correctBoolean\":true}");
    }
}
