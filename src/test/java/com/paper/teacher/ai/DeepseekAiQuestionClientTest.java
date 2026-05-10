package com.paper.teacher.ai;

import com.paper.teacher.config.DeepseekAiProperties;
import com.paper.teacher.constant.enums.DifficultyEnum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.ai.AiQuestionGenerationRequest;
import com.paper.teacher.modules.ai.AiQuestionGenerationResponse;
import com.paper.teacher.modules.ai.DeepseekAiQuestionClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepseekAiQuestionClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsJsonModeRequestAndParsesQuestions() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeepseekAiProperties properties = properties();
        DeepseekAiQuestionClient client = new DeepseekAiQuestionClient(builder, objectMapper, properties);

        String questionJson = """
                {
                  "questions": [
                    {
                      "questionType": "TRUE_FALSE",
                      "difficulty": "EASY",
                      "stem": "1 是数字。",
                      "contentJson": "{\\"statement\\":\\"1 是数字。\\"}",
                      "answerJson": "{\\"correctBoolean\\":true}",
                      "analysis": "1 是阿拉伯数字。"
                    }
                  ]
                }
                """;
        String deepseekResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", questionJson)))
        ));

        server.expect(requestTo("https://api.deepseek.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(content().string(containsString("\"model\":\"deepseek-chat\"")))
                .andExpect(content().string(containsString("\"response_format\":{\"type\":\"json_object\"}")))
                .andExpect(content().string(containsString("只输出 JSON")))
                .andExpect(content().string(containsString("Unit 3 / Measurement; Unit 3 / Kilometer")))
                .andExpect(content().string(containsString("correctBoolean")))
                .andRespond(withSuccess(deepseekResponse, MediaType.APPLICATION_JSON));

        List<AiQuestionGenerationResponse> responses = client.generate(request());

        assertThat(responses).containsExactly(new AiQuestionGenerationResponse(
                QuestionTypeEnum.TRUE_FALSE,
                DifficultyEnum.EASY,
                "1 是数字。",
                "{\"statement\":\"1 是数字。\"}",
                "{\"correctBoolean\":true}",
                "1 是阿拉伯数字。"
        ));
        server.verify();
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingDeepseek() {
        DeepseekAiProperties properties = properties();
        properties.setApiKey("");
        DeepseekAiQuestionClient client = new DeepseekAiQuestionClient(RestClient.builder(), objectMapper, properties);

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DeepSeek API Key");
    }

    @Test
    void rejectsUnexpectedQuestionCount() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeepseekAiQuestionClient client = new DeepseekAiQuestionClient(builder, objectMapper, properties());
        String deepseekResponse = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", "{\"questions\":[]}")))
        ));

        server.expect(requestTo("https://api.deepseek.test/chat/completions"))
                .andRespond(withSuccess(deepseekResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DeepSeek 返回题目数量");
        server.verify();
    }

    private DeepseekAiProperties properties() {
        DeepseekAiProperties properties = new DeepseekAiProperties();
        properties.setBaseUrl("https://api.deepseek.test");
        properties.setApiKey("sk-test");
        properties.setModel("deepseek-chat");
        return properties;
    }

    private AiQuestionGenerationRequest request() {
        return new AiQuestionGenerationRequest(
                "Grade 3",
                "PEP",
                "MATH",
                "Volume 1",
                "精确章节：Unit 3 / Measurement; Unit 3 / Kilometer",
                QuestionTypeEnum.TRUE_FALSE,
                DifficultyEnum.EASY,
                1,
                BigDecimal.TEN
        );
    }
}
