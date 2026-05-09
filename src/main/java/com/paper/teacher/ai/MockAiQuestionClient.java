package com.paper.teacher.ai;

import com.paper.teacher.question.QuestionType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock")
public class MockAiQuestionClient implements AiQuestionClient {
    @Override
    public List<AiQuestionGenerationResponse> generate(AiQuestionGenerationRequest request) {
        List<AiQuestionGenerationResponse> responses = new ArrayList<>();
        for (int i = 1; i <= request.count(); i++) {
            responses.add(new AiQuestionGenerationResponse(
                    request.questionType(),
                    request.difficulty(),
                    request.unit() + " " + title(request.questionType()) + " AI补题 " + i,
                    content(request.questionType(), i),
                    answer(request.questionType(), i),
                    "本题由本地 mock AI 生成，用于补足题库缺口。"
            ));
        }
        return responses;
    }

    private String title(QuestionType type) {
        return switch (type) {
            case SINGLE_CHOICE -> "选择题";
            case TRUE_FALSE -> "判断题";
            case FILL_BLANK -> "填空题";
            case MATCHING -> "连线题";
            case DICTATION -> "默写题";
        };
    }

    private String content(QuestionType type, int index) {
        return switch (type) {
            case SINGLE_CHOICE -> "{\"options\":[\"A. 1\",\"B. 2\",\"C. 3\",\"D. 4\"]}";
            case TRUE_FALSE -> "{\"statement\":\"示例判断题 " + index + "\"}";
            case FILL_BLANK -> "{\"blanks\":[\"blank1\"]}";
            case MATCHING -> "{\"leftItems\":[\"左1\",\"左2\"],\"rightItems\":[\"右1\",\"右2\"]}";
            case DICTATION -> "{\"prompt\":\"请默写示例词语 " + index + "\"}";
        };
    }

    private String answer(QuestionType type, int index) {
        return switch (type) {
            case SINGLE_CHOICE -> "{\"correctOption\":\"B\"}";
            case TRUE_FALSE -> "{\"correctBoolean\":true}";
            case FILL_BLANK -> "{\"acceptedAnswers\":[[\"答案" + index + "\"]]}";
            case MATCHING -> "{\"pairs\":[{\"left\":\"左1\",\"right\":\"右1\"},{\"left\":\"左2\",\"right\":\"右2\"}]}";
            case DICTATION -> "{\"expectedText\":\"示例词语" + index + "\"}";
        };
    }
}
