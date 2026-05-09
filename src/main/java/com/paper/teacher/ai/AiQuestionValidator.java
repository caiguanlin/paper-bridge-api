package com.paper.teacher.ai;

import com.paper.teacher.question.QuestionValidator;
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
