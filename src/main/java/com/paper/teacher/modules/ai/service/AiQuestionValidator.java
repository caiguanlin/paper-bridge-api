package com.paper.teacher.modules.ai.service;

import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;

import com.paper.teacher.modules.question.service.QuestionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiQuestionValidator {
    private final QuestionValidator questionValidator;

    public void validate(AiQuestionGenerationResponse response) {
        questionValidator.validate(response.questionType(), response.contentJson(), response.answerJson());
    }
}
