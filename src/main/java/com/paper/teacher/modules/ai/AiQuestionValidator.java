package com.paper.teacher.modules.ai;

import com.paper.teacher.modules.question.QuestionValidator;
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
