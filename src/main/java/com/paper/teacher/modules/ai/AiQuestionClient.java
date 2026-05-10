package com.paper.teacher.modules.ai;

import java.util.List;

public interface AiQuestionClient {
    List<AiQuestionGenerationResponse> generate(AiQuestionGenerationRequest request);
}
