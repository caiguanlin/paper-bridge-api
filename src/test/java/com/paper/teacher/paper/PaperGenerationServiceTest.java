package com.paper.teacher.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.ai.AiQuestionClient;
import com.paper.teacher.ai.AiQuestionGenerationRequest;
import com.paper.teacher.ai.AiQuestionGenerationResponse;
import com.paper.teacher.ai.AiQuestionValidator;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.paper.dto.PaperGenerateRequest;
import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionRepository;
import com.paper.teacher.question.QuestionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperGenerationServiceTest {
    private final PaperRepository paperRepository = mock(PaperRepository.class);
    private final PaperSectionRepository sectionRepository = mock(PaperSectionRepository.class);
    private final PaperQuestionRepository paperQuestionRepository = mock(PaperQuestionRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final AiQuestionClient aiQuestionClient = mock(AiQuestionClient.class);
    private final AiQuestionValidator aiQuestionValidator = mock(AiQuestionValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaperGenerationService service = new PaperGenerationService(
            paperRepository, sectionRepository, paperQuestionRepository, questionRepository, aiQuestionClient, aiQuestionValidator, objectMapper
    );

    @Test
    void rejectsScoreMismatch() {
        PaperGenerateRequest request = request(BigDecimal.valueOf(100), 9, BigDecimal.TEN);

        assertThatThrownBy(() -> service.preview(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("总分必须等于各题型小计之和");
    }

    @Test
    void aiSupplementsOnlyMissingQuestionCount() {
        AtomicReference<Paper> savedPaper = new AtomicReference<>();
        List<PaperSection> sections = new ArrayList<>();
        List<PaperQuestion> questions = new ArrayList<>();

        when(paperRepository.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            paper.setId(10L);
            paper.setUpdatedAt(LocalDateTime.now());
            savedPaper.set(paper);
            return 1;
        });
        when(paperRepository.selectById(10L)).thenAnswer(invocation -> savedPaper.get());
        when(sectionRepository.insert(any(PaperSection.class))).thenAnswer(invocation -> {
            PaperSection section = invocation.getArgument(0);
            section.setId(20L);
            sections.add(section);
            return 1;
        });
        when(sectionRepository.selectList(any())).thenAnswer(invocation -> sections);
        when(paperQuestionRepository.insert(any(PaperQuestion.class))).thenAnswer(invocation -> {
            PaperQuestion question = invocation.getArgument(0);
            question.setId((long) questions.size() + 1);
            questions.add(question);
            return 1;
        });
        when(paperQuestionRepository.selectList(any())).thenAnswer(invocation -> questions);
        when(questionRepository.selectList(any())).thenReturn(List.of());
        when(aiQuestionClient.generate(any())).thenReturn(List.of(
                aiQuestion(1),
                aiQuestion(2)
        ));

        PaperGenerateRequest request = request(BigDecimal.valueOf(20), 2, BigDecimal.TEN);
        var response = service.generate(1L, request);

        ArgumentCaptor<AiQuestionGenerationRequest> captor = ArgumentCaptor.forClass(AiQuestionGenerationRequest.class);
        verify(aiQuestionClient).generate(captor.capture());
        assertThat(captor.getValue().count()).isEqualTo(2);
        assertThat(captor.getValue().scopeDescription()).contains("第三单元 / 测量", "第三单元 / 千米的认识");
        assertThat(savedPaper.get().getScopeType()).isEqualTo(PaperScopeType.CHAPTERS);
        assertThat(savedPaper.get().getChapter()).isEqualTo("第三单元 / 测量, 第三单元 / 千米的认识");
        assertThat(response.sections()).hasSize(1);
        assertThat(response.sections().getFirst().questions()).hasSize(2);
    }

    @Test
    void regenerateSplitsSavedChapterDisplayIntoChapterScope() {
        Paper original = new Paper();
        original.setId(5L);
        original.setOwnerUserId(1L);
        original.setTitle("Original");
        original.setGrade("Grade 3");
        original.setPublisher("PEP");
        original.setSubject("MATH");
        original.setVolume("Volume 1");
        original.setUnit("Unit 1, Unit 2");
        original.setChapter("全部章节");
        original.setScopeType(PaperScopeType.UNITS);
        original.setScopePayloadJson("""
                {"scopeType":"UNITS","units":["Unit 1","Unit 2"],"chapters":null}
                """);
        original.setTotalScore(BigDecimal.TEN);
        original.setStatus(PaperStatus.DRAFT);

        PaperSection originalSection = new PaperSection();
        originalSection.setId(6L);
        originalSection.setTitle("True or False");
        originalSection.setQuestionType(QuestionType.TRUE_FALSE);
        originalSection.setQuestionCount(1);
        originalSection.setScorePerQuestion(BigDecimal.TEN);
        originalSection.setSubtotalScore(BigDecimal.TEN);
        originalSection.setSortOrder(1);

        AtomicReference<Paper> generatedPaper = new AtomicReference<>();
        List<PaperSection> generatedSections = new ArrayList<>();
        List<PaperQuestion> generatedQuestions = new ArrayList<>();
        AtomicInteger sectionSelectCalls = new AtomicInteger();

        when(paperRepository.selectById(5L)).thenReturn(original);
        when(paperRepository.insert(any(Paper.class))).thenAnswer(invocation -> {
            Paper paper = invocation.getArgument(0);
            paper.setId(10L);
            generatedPaper.set(paper);
            return 1;
        });
        when(paperRepository.selectById(10L)).thenAnswer(invocation -> generatedPaper.get());
        when(sectionRepository.selectList(any())).thenAnswer(invocation ->
                sectionSelectCalls.getAndIncrement() == 0 ? List.of(originalSection) : generatedSections);
        when(sectionRepository.insert(any(PaperSection.class))).thenAnswer(invocation -> {
            PaperSection section = invocation.getArgument(0);
            section.setId(20L);
            generatedSections.add(section);
            return 1;
        });
        when(paperQuestionRepository.selectList(any())).thenAnswer(invocation -> generatedQuestions);
        when(paperQuestionRepository.insert(any(PaperQuestion.class))).thenAnswer(invocation -> {
            PaperQuestion question = invocation.getArgument(0);
            generatedQuestions.add(question);
            return 1;
        });
        when(questionRepository.selectList(any())).thenReturn(List.of());
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(1)));

        service.regenerate(1L, 5L);

        ArgumentCaptor<AiQuestionGenerationRequest> captor = ArgumentCaptor.forClass(AiQuestionGenerationRequest.class);
        verify(aiQuestionClient).generate(captor.capture());
        assertThat(captor.getValue().scopeDescription()).isEqualTo("单元：Unit 1, Unit 2（全部章节）");
        assertThat(generatedPaper.get().getScopeType()).isEqualTo(PaperScopeType.UNITS);
        assertThat(generatedPaper.get().getUnit()).isEqualTo("Unit 1, Unit 2");
        assertThat(generatedPaper.get().getChapter()).isEqualTo("全部章节");
    }

    private PaperGenerateRequest request(BigDecimal totalScore, int count, BigDecimal scorePerQuestion) {
        return new PaperGenerateRequest(
                "三年级数学测评",
                "三年级",
                "人教版",
                "MATH",
                "上册",
                PaperScopeType.CHAPTERS,
                null,
                List.of(
                        new PaperGenerateRequest.ChapterScope("第三单元", "测量"),
                        new PaperGenerateRequest.ChapterScope("第三单元", "千米的认识")
                ),
                totalScore,
                GenerationStrategy.BANK_WITH_AI,
                Difficulty.MEDIUM,
                List.of(new PaperGenerateRequest.SectionRequest("判断题", QuestionType.TRUE_FALSE, count, scorePerQuestion))
        );
    }

    private AiQuestionGenerationResponse aiQuestion(int index) {
        return new AiQuestionGenerationResponse(
                QuestionType.TRUE_FALSE,
                Difficulty.MEDIUM,
                "AI 判断题 " + index,
                "{\"statement\":\"AI 判断题\"}",
                "{\"correctBoolean\":true}",
                "解析"
        );
    }
}
