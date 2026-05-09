package com.paper.teacher.ai;

import java.util.List;

public interface AiQuestionClient {
    List<AiQuestionGenerationResponse> generate(AiQuestionGenerationRequest request);
}
