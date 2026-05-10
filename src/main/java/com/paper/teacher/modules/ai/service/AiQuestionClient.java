package com.paper.teacher.modules.ai.service;

import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;

import com.paper.teacher.modules.ai.dto.AiQuestionGenerationRequest;

import java.util.List;

public interface AiQuestionClient {
    List<AiQuestionGenerationResponse> generate(AiQuestionGenerationRequest request);
}
