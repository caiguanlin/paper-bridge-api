package com.paper.teacher.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.config.DeepseekAiProperties;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationRequest;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepseekAiQuestionClientTest {
    private static final AiQuestionGenerationRequest REQUEST = new AiQuestionGenerationRequest(
            "三年级", "人教版", "CHINESE", "上册", "单元：第一单元（全部章节）",
            QuestionTypeEnum.SINGLE_CHOICE, DifficultyEnum.EASY, 1, new BigDecimal("2"));

    @Test
    void failsFastWhenApiKeyMissing() {
        DeepseekAiProperties properties = properties("");
        DeepseekAiQuestionClient client =
                new DeepseekAiQuestionClient(RestClient.builder(), new ObjectMapper(), properties);

        BusinessException exception = assertThrows(BusinessException.class, () -> client.generate(REQUEST));
        assertEquals("DeepSeek API Key 未配置", exception.getMessage());
    }

    @Test
    void parsesGeneratedQuestions() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer secret-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"deepseek-chat\"")))
                .andRespond(withSuccess(chatCompletion("""
                        {"questions":[{"questionType":"SINGLE_CHOICE","difficulty":"EASY","stem":"题干",
                        "contentJson":"{}","answerJson":"{}","analysis":"解析"}]}"""), MediaType.APPLICATION_JSON));

        List<AiQuestionGenerationResponse> questions = new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST);

        server.verify();
        assertEquals(1, questions.size());
        assertEquals(QuestionTypeEnum.SINGLE_CHOICE, questions.getFirst().questionType());
        assertEquals("题干", questions.getFirst().stem());
        assertEquals("解析", questions.getFirst().analysis());
    }

    @Test
    void rejectsResponseWithoutChoices() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class, () -> new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST));
        assertTrue(exception.getMessage().contains("DeepSeek 响应为空"));
    }

    @Test
    void rejectsResponseWithBlankContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"  \"}}]}",
                        MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class, () -> new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST));
        assertTrue(exception.getMessage().contains("DeepSeek 未返回题目内容"));
    }

    @Test
    void rejectsResponseWithoutQuestionsField() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions"))
                .andRespond(withSuccess(chatCompletion("{}"), MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class, () -> new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST));
        assertTrue(exception.getMessage().contains("DeepSeek 未返回 questions"));
    }

    @Test
    void rejectsResponseWithUnexpectedQuestionCount() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions"))
                .andRespond(withSuccess(chatCompletion("{\"questions\":[]}"), MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(BusinessException.class, () -> new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST));
        assertTrue(exception.getMessage().contains("期望 1，实际 0"));
    }

    @Test
    void wrapsHttpFailuresAsBusinessException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.test/v1/chat/completions")).andRespond(withServerError());

        BusinessException exception = assertThrows(BusinessException.class, () -> new DeepseekAiQuestionClient(
                builder, new ObjectMapper(), properties("secret-key")).generate(REQUEST));
        assertTrue(exception.getMessage().startsWith("DeepSeek 生成题目失败："));
    }

    private static DeepseekAiProperties properties(String apiKey) {
        DeepseekAiProperties properties = new DeepseekAiProperties();
        properties.setBaseUrl("https://api.deepseek.test/v1/");
        properties.setApiKey(apiKey);
        return properties;
    }

    private static String chatCompletion(String content) {
        try {
            String json = new ObjectMapper().writeValueAsString(content);
            return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + json + "}}]}";
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
