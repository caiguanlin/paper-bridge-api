package com.paper.teacher.modules.ai;

import com.paper.teacher.config.DeepseekAiProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "deepseek")
public class DeepseekAiQuestionClient implements AiQuestionClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DeepseekAiProperties properties;

    public DeepseekAiQuestionClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DeepseekAiProperties properties
    ) {
        this.restClient = restClientBuilder.baseUrl(normalizeBaseUrl(properties.getBaseUrl())).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<AiQuestionGenerationResponse> generate(AiQuestionGenerationRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException("DeepSeek API Key 未配置");
        }

        try {
            DeepseekChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(chatRequest(request))
                    .retrieve()
                    .body(DeepseekChatResponse.class);
            return parseQuestions(response, request.count());
        } catch (RestClientException | IllegalArgumentException | JsonProcessingException ex) {
            throw new BusinessException("DeepSeek 生成题目失败：" + ex.getMessage());
        }
    }

    private DeepseekChatRequest chatRequest(AiQuestionGenerationRequest request) {
        return new DeepseekChatRequest(
                properties.getModel(),
                List.of(
                        new Message("system", systemPrompt()),
                        new Message("user", userPrompt(request))
                ),
                Map.of("type", "json_object"),
                properties.getTemperature()
        );
    }

    private List<AiQuestionGenerationResponse> parseQuestions(DeepseekChatResponse response, int expectedCount)
            throws JsonProcessingException {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("DeepSeek 响应为空");
        }
        Message message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new IllegalArgumentException("DeepSeek 未返回题目内容");
        }
        GeneratedQuestions generated = objectMapper.readValue(message.content(), GeneratedQuestions.class);
        if (generated.questions() == null) {
            throw new IllegalArgumentException("DeepSeek 未返回 questions");
        }
        if (generated.questions().size() != expectedCount) {
            throw new IllegalArgumentException("DeepSeek 返回题目数量不正确，期望 "
                    + expectedCount + "，实际 " + generated.questions().size());
        }
        return generated.questions();
    }

    private String systemPrompt() {
        return """
                你是小学教师命题助手。只输出 JSON，不要 Markdown，不要代码块，不要解释。
                题目必须适合小学教材范围，字段必须严格匹配用户给出的 JSON schema。
                contentJson 和 answerJson 字段本身必须是合法 JSON 字符串。
                """;
    }

    private String userPrompt(AiQuestionGenerationRequest request) {
        return """
                请生成 %d 道题目。

                范围：
                年级：%s
                出版社：%s
                学科：%s
                册别：%s
                教材范围：%s
                题型：%s
                难度：%s
                每题分值：%s

                题型 JSON 规则：
                %s

                只输出 JSON，格式如下：
                {
                  "questions": [
                    {
                      "questionType": "%s",
                      "difficulty": "%s",
                      "stem": "题干",
                      "contentJson": "题型内容 JSON 字符串",
                      "answerJson": "答案 JSON 字符串",
                      "analysis": "解析"
                    }
                  ]
                }
                """.formatted(
                request.count(),
                request.grade(),
                request.publisher(),
                request.subject(),
                request.volume(),
                request.scopeDescription(),
                request.questionType(),
                request.difficulty(),
                request.scorePerQuestion(),
                typeGuidance(request),
                request.questionType(),
                request.difficulty()
        );
    }

    private String typeGuidance(AiQuestionGenerationRequest request) {
        return switch (request.questionType()) {
            case SINGLE_CHOICE -> """
                    contentJson: {"options":["A. 选项","B. 选项","C. 选项","D. 选项"]}
                    answerJson: {"correctOption":"A"}
                    """;
            case TRUE_FALSE -> """
                    contentJson: {"statement":"判断陈述"}
                    answerJson: {"correctBoolean":true}
                    """;
            case FILL_BLANK -> """
                    contentJson: {"blanks":["blank1","blank2"]}
                    answerJson: {"acceptedAnswers":[["答案1"],["答案2"]]}
                    """;
            case MATCHING -> """
                    contentJson: {"leftItems":["左项1","左项2"],"rightItems":["右项1","右项2"]}
                    answerJson: {"pairs":[{"left":"左项1","right":"右项1"}]}
                    """;
            case DICTATION -> """
                    contentJson: {"prompt":"默写提示"}
                    answerJson: {"expectedText":"标准默写内容"}
                    """;
        };
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record DeepseekChatRequest(
            String model,
            List<Message> messages,
            @JsonProperty("response_format")
            Map<String, String> responseFormat,
            double temperature
    ) {
    }

    private record Message(String role, String content) {
    }

    private record DeepseekChatResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record GeneratedQuestions(List<AiQuestionGenerationResponse> questions) {
    }
}
