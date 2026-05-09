package com.paper.teacher.ai;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionType;

public record AiQuestionGenerationResponse(
        QuestionType questionType,
        Difficulty difficulty,
        String stem,
        String contentJson,
        String answerJson,
        String analysis
) {
}
