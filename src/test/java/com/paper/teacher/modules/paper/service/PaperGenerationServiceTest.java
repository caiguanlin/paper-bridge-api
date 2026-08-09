package com.paper.teacher.modules.paper.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.GenerationStrategyEnum;
import com.paper.teacher.constant.enums.PaperScopeTypeEnum;
import com.paper.teacher.constant.enums.PaperStatusEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationRequest;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;
import com.paper.teacher.modules.ai.service.AiQuestionClient;
import com.paper.teacher.modules.ai.service.AiQuestionValidator;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest;
import com.paper.teacher.modules.paper.dto.PaperPlanPreview;
import com.paper.teacher.modules.paper.dto.PaperResponse;
import com.paper.teacher.modules.paper.dto.PaperSummaryResponse;
import com.paper.teacher.modules.paper.entity.Paper;
import com.paper.teacher.modules.paper.entity.PaperQuestion;
import com.paper.teacher.modules.paper.entity.PaperSection;
import com.paper.teacher.modules.paper.repository.PaperQuestionRepository;
import com.paper.teacher.modules.paper.repository.PaperRepository;
import com.paper.teacher.modules.paper.repository.PaperSectionRepository;
import com.paper.teacher.modules.question.entity.Question;
import com.paper.teacher.modules.question.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaperGenerationServiceTest {
    private static final long OWNER_ID = 4L;

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private PaperQuestionRepository paperQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AiQuestionClient aiQuestionClient;
    @Mock
    private AiQuestionValidator aiQuestionValidator;

    private PaperGenerationService service() {
        return new PaperGenerationService(paperRepository, paperSectionRepository, paperQuestionRepository,
                questionRepository, aiQuestionClient, aiQuestionValidator, new ObjectMapper());
    }

    @Test
    void previewReportsBankAvailabilityAndSupplement() {
        when(questionRepository.selectCount(any(Wrapper.class))).thenReturn(3L);

        PaperPlanPreview preview = service().preview(OWNER_ID, request(GenerationStrategyEnum.BANK_FIRST));

        assertEquals(new BigDecimal("10"), preview.totalScore());
        assertEquals(new BigDecimal("10"), preview.subtotalScore());
        PaperPlanPreview.SectionPreview section = preview.sections().getFirst();
        assertEquals(5, section.requiredCount());
        assertEquals(3, section.availableBankCount());
        assertEquals(2, section.aiSupplementCount());
        assertEquals(new BigDecimal("10"), section.subtotalScore());
    }

    @Test
    void previewSkipsBankLookupForAiOnlyStrategy() {
        PaperPlanPreview preview = service().preview(OWNER_ID, request(GenerationStrategyEnum.AI_ONLY));

        verify(questionRepository, never()).selectCount(any());
        assertEquals(0, preview.sections().getFirst().availableBankCount());
        assertEquals(5, preview.sections().getFirst().aiSupplementCount());
    }

    @Test
    void previewNeverSupplementsForBankOnlyStrategy() {
        when(questionRepository.selectCount(any(Wrapper.class))).thenReturn(1L);

        PaperPlanPreview preview = service().preview(OWNER_ID, request(GenerationStrategyEnum.BANK_ONLY));

        assertEquals(0, preview.sections().getFirst().aiSupplementCount());
    }

    @Test
    void previewRejectsTotalScoreMismatch() {
        PaperGenerateRequest request = new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.VOLUME, null, null,
                new BigDecimal("99"), GenerationStrategyEnum.BANK_ONLY, null, sections());

        BusinessException exception = assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request));

        assertEquals("总分必须等于各题型小计之和，当前小计为 10", exception.getMessage());
    }

    @Test
    void previewRejectsMissingScopeType() {
        PaperGenerateRequest request = new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", null, null, null,
                new BigDecimal("10"), GenerationStrategyEnum.BANK_ONLY, null, sections());

        assertEquals("组卷范围类型不能为空",
                assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request)).getMessage());
    }

    @Test
    void previewRejectsEmptyChapterScope() {
        PaperGenerateRequest request = scopedRequest(PaperScopeTypeEnum.CHAPTERS, null, List.of());

        assertEquals("CHAPTERS 范围必须至少选择一个章节",
                assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request)).getMessage());
    }

    @Test
    void previewRejectsBlankChapterScope() {
        PaperGenerateRequest request = scopedRequest(PaperScopeTypeEnum.CHAPTERS, null,
                List.of(new PaperGenerateRequest.ChapterScope("第一单元", " ")));

        assertEquals("CHAPTERS 范围中的单元和章节不能为空",
                assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request)).getMessage());
    }

    @Test
    void previewRejectsEmptyUnitScope() {
        PaperGenerateRequest request = scopedRequest(PaperScopeTypeEnum.UNITS, List.of(), null);

        assertEquals("UNITS 范围必须至少选择一个单元",
                assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request)).getMessage());
    }

    @Test
    void previewRejectsBlankUnitScope() {
        PaperGenerateRequest request = scopedRequest(PaperScopeTypeEnum.UNITS, List.of(" "), null);

        assertEquals("UNITS 范围中的单元不能为空",
                assertThrows(BusinessException.class, () -> service().preview(OWNER_ID, request)).getMessage());
    }

    @Test
    void generateSnapshotsBankQuestionsThenSupplementsWithAi() {
        Question bankQuestion = bankQuestion(31L, 2);
        when(questionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(bankQuestion));
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(paper(70L, OWNER_ID));

        PaperResponse response = service().generate(OWNER_ID, request(GenerationStrategyEnum.BANK_FIRST));

        ArgumentCaptor<AiQuestionGenerationRequest> aiRequest = ArgumentCaptor.forClass(AiQuestionGenerationRequest.class);
        verify(aiQuestionClient).generate(aiRequest.capture());
        assertEquals(4, aiRequest.getValue().count());
        assertEquals("整册", aiRequest.getValue().scopeDescription());
        assertEquals(DifficultyEnum.MEDIUM, aiRequest.getValue().difficulty());
        verify(aiQuestionValidator, times(4)).validate(any());

        ArgumentCaptor<PaperQuestion> snapshots = ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepository, times(5)).insert(snapshots.capture());
        PaperQuestion fromBank = snapshots.getAllValues().getFirst();
        assertEquals(31L, fromBank.getSourceQuestionId());
        assertEquals(QuestionSourceEnum.MANUAL, fromBank.getSource());
        assertEquals(1, fromBank.getSortOrder());
        PaperQuestion fromAi = snapshots.getAllValues().get(1);
        assertNull(fromAi.getSourceQuestionId());
        assertEquals(QuestionSourceEnum.AI, fromAi.getSource());
        assertEquals(2, fromAi.getSortOrder());

        assertEquals(3, bankQuestion.getUsageCount());
        verify(questionRepository).updateById(bankQuestion);
        assertEquals(70L, response.id());
    }

    @Test
    void generatePersistsScopeDisplayAndPayloadForChapterScope() {
        PaperGenerateRequest request = new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.CHAPTERS, null,
                List.of(new PaperGenerateRequest.ChapterScope("第一单元", "第一课"),
                        new PaperGenerateRequest.ChapterScope("第一单元", "第二课")),
                new BigDecimal("10"), GenerationStrategyEnum.AI_ONLY, DifficultyEnum.HARD, sections());
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(paper(70L, OWNER_ID));

        service().generate(OWNER_ID, request);

        ArgumentCaptor<Paper> paper = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).insert(paper.capture());
        assertEquals("第一单元", paper.getValue().getUnit());
        assertEquals("第一单元 / 第一课, 第一单元 / 第二课", paper.getValue().getChapter());
        assertEquals(PaperStatusEnum.DRAFT, paper.getValue().getStatus());
        assertTrue(paper.getValue().getScopePayloadJson().contains("\"scopeType\":\"CHAPTERS\""));
        ArgumentCaptor<AiQuestionGenerationRequest> aiRequest = ArgumentCaptor.forClass(AiQuestionGenerationRequest.class);
        verify(aiQuestionClient).generate(aiRequest.capture());
        assertEquals("精确章节：第一单元 / 第一课; 第一单元 / 第二课", aiRequest.getValue().scopeDescription());
        assertEquals(DifficultyEnum.HARD, aiRequest.getValue().difficulty());
    }

    @Test
    void generateUsesUnitScopeDisplay() {
        PaperGenerateRequest request = new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.UNITS, List.of("第一单元", "第二单元"),
                null, new BigDecimal("10"), GenerationStrategyEnum.AI_ONLY, null, sections());
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(paper(70L, OWNER_ID));

        service().generate(OWNER_ID, request);

        ArgumentCaptor<Paper> paper = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).insert(paper.capture());
        assertEquals("第一单元, 第二单元", paper.getValue().getUnit());
        assertEquals("全部章节", paper.getValue().getChapter());
        ArgumentCaptor<AiQuestionGenerationRequest> aiRequest = ArgumentCaptor.forClass(AiQuestionGenerationRequest.class);
        verify(aiQuestionClient).generate(aiRequest.capture());
        assertEquals("单元：第一单元, 第二单元（全部章节）", aiRequest.getValue().scopeDescription());
    }

    @Test
    void generateFailsWhenBankOnlyStrategyHasTooFewQuestions() {
        when(questionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(bankQuestion(31L, null)));
        stubPaperInsert();
        stubSectionInsert();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service().generate(OWNER_ID, request(GenerationStrategyEnum.BANK_ONLY)));

        assertEquals("一、选择题题库数量不足，缺少 4 道题", exception.getMessage());
        verify(aiQuestionClient, never()).generate(any());
    }

    @Test
    void generatePrefersLeastUsedBankQuestions() {
        Question used = bankQuestion(1L, 5);
        Question unused = bankQuestion(2L, null);
        Question usedOnce = bankQuestion(3L, 1);
        when(questionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(used, usedOnce, unused));
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(paper(70L, OWNER_ID));

        service().generate(OWNER_ID, request(GenerationStrategyEnum.BANK_FIRST));

        ArgumentCaptor<PaperQuestion> snapshots = ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepository, times(5)).insert(snapshots.capture());
        assertEquals(List.of(2L, 3L, 1L), snapshots.getAllValues().subList(0, 3).stream()
                .map(PaperQuestion::getSourceQuestionId).toList());
    }

    @Test
    void listMapsPapersToSummaries() {
        Paper paper = paper(70L, OWNER_ID);
        when(paperRepository.selectList(any(Wrapper.class))).thenReturn(List.of(paper));

        List<PaperSummaryResponse> summaries = service().list(OWNER_ID);

        assertEquals(1, summaries.size());
        assertEquals(70L, summaries.getFirst().id());
        assertEquals("期中卷", summaries.getFirst().title());
    }

    @Test
    void copyDuplicatesSectionsAndQuestions() {
        Paper original = paper(70L, OWNER_ID);
        when(paperRepository.selectById(70L)).thenReturn(original);
        when(paperRepository.insert(any(Paper.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Paper.class).setId(71L);
            return 1;
        });
        when(paperRepository.selectById(71L)).thenAnswer(invocation -> paper(71L, OWNER_ID));
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section(700L)));
        when(paperQuestionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(paperQuestion(7000L)));
        stubSectionInsert();

        PaperResponse response = service().copy(OWNER_ID, 70L);

        ArgumentCaptor<Paper> copy = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).insert(copy.capture());
        assertEquals("期中卷 (副本)", copy.getValue().getTitle());
        assertEquals(PaperStatusEnum.DRAFT, copy.getValue().getStatus());
        ArgumentCaptor<PaperQuestion> copiedQuestion = ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepository).insert(copiedQuestion.capture());
        assertEquals(71L, copiedQuestion.getValue().getPaperId());
        assertEquals("题干", copiedQuestion.getValue().getStemSnapshot());
        assertEquals(1, response.sections().size());
        assertEquals(1, response.sections().getFirst().questions().size());
    }

    @Test
    void regenerateClearsQuestionsAndRebuildsFromStoredScope() {
        Paper original = paper(70L, OWNER_ID);
        original.setScopePayloadJson("{\"scopeType\":\"UNITS\",\"units\":[\"第一单元\"],\"chapters\":null}");
        when(paperRepository.selectById(70L)).thenReturn(original);
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section(700L)));
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(original);

        service().regenerate(OWNER_ID, 70L);

        verify(paperQuestionRepository).delete(any(LambdaQueryWrapper.class));
        verify(paperSectionRepository).delete(any(LambdaQueryWrapper.class));
        ArgumentCaptor<Paper> regenerated = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).insert(regenerated.capture());
        assertEquals("期中卷 (重新组卷)", regenerated.getValue().getTitle());
        assertEquals(PaperScopeTypeEnum.UNITS, regenerated.getValue().getScopeType());
        assertEquals("第一单元", regenerated.getValue().getUnit());
    }

    @Test
    void regenerateFallsBackToLegacyUnitAndChapterColumns() {
        Paper original = paper(70L, OWNER_ID);
        original.setScopePayloadJson(" ");
        original.setUnit("第一单元");
        original.setChapter("第一课, 第二课");
        when(paperRepository.selectById(70L)).thenReturn(original);
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section(700L)));
        when(questionRepository.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(aiQuestionClient.generate(any())).thenReturn(List.of(aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion(), aiQuestion()));
        stubPaperInsert();
        stubSectionInsert();
        stubLoadPaper(original);

        service().regenerate(OWNER_ID, 70L);

        ArgumentCaptor<Paper> regenerated = ArgumentCaptor.forClass(Paper.class);
        verify(paperRepository).insert(regenerated.capture());
        assertEquals(PaperScopeTypeEnum.CHAPTERS, regenerated.getValue().getScopeType());
        assertEquals("第一单元 / 第一课, 第一单元 / 第二课", regenerated.getValue().getChapter());
    }

    @Test
    void regenerateRejectsUnparsableScopePayload() {
        Paper original = paper(70L, OWNER_ID);
        original.setScopePayloadJson("{not-json");
        when(paperRepository.selectById(70L)).thenReturn(original);
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section(700L)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service().regenerate(OWNER_ID, 70L));

        assertTrue(exception.getMessage().startsWith("组卷范围解析失败："));
    }

    @Test
    void saveMarksPaperAsSavedOnlyOnce() {
        Paper paper = paper(70L, OWNER_ID);
        when(paperRepository.selectById(70L)).thenReturn(paper);

        service().save(OWNER_ID, 70L);
        assertEquals(PaperStatusEnum.SAVED, paper.getStatus());
        verify(paperRepository).updateById(paper);

        service().save(OWNER_ID, 70L);
        verify(paperRepository, times(1)).updateById(paper);
    }

    @Test
    void deleteRemovesQuestionsSectionsAndPaper() {
        Paper paper = paper(70L, OWNER_ID);
        when(paperRepository.selectById(70L)).thenReturn(paper);

        service().delete(OWNER_ID, 70L);

        verify(paperQuestionRepository).delete(any(LambdaQueryWrapper.class));
        verify(paperSectionRepository).delete(any(LambdaQueryWrapper.class));
        verify(paperRepository).deleteById(paper);
    }

    @Test
    void requirePaperRejectsMissingPaper() {
        when(paperRepository.selectById(70L)).thenReturn(null);

        assertEquals("试卷不存在",
                assertThrows(BusinessException.class, () -> service().loadPaper(OWNER_ID, 70L)).getMessage());
    }

    @Test
    void requirePaperRejectsPaperOwnedByAnotherTeacher() {
        when(paperRepository.selectById(70L)).thenReturn(paper(70L, 999L));

        assertEquals("试卷不存在",
                assertThrows(BusinessException.class, () -> service().loadPaper(OWNER_ID, 70L)).getMessage());
    }

    private void stubPaperInsert() {
        when(paperRepository.insert(any(Paper.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Paper.class).setId(70L);
            return 1;
        });
    }

    private void stubSectionInsert() {
        when(paperSectionRepository.insert(any(PaperSection.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PaperSection.class).setId(700L);
            return 1;
        });
    }

    private void stubLoadPaper(Paper paper) {
        when(paperRepository.selectById(70L)).thenReturn(paper);
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section(700L)));
        when(paperQuestionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(paperQuestion(7000L)));
    }

    private static PaperGenerateRequest request(GenerationStrategyEnum strategy) {
        return new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.VOLUME, null, null,
                new BigDecimal("10"), strategy, null, sections());
    }

    private static PaperGenerateRequest scopedRequest(
            PaperScopeTypeEnum scopeType, List<String> units, List<PaperGenerateRequest.ChapterScope> chapters) {
        return new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", scopeType, units, chapters,
                new BigDecimal("10"), GenerationStrategyEnum.BANK_ONLY, null, sections());
    }

    private static List<PaperGenerateRequest.SectionRequest> sections() {
        return List.of(new PaperGenerateRequest.SectionRequest(
                "一、选择题", QuestionTypeEnum.SINGLE_CHOICE, 5, new BigDecimal("2")));
    }

    private static Question bankQuestion(Long id, Integer usageCount) {
        Question question = new Question();
        question.setId(id);
        question.setStem("题干" + id);
        question.setContentJson("{\"options\":[\"A\",\"B\"]}");
        question.setAnswerJson("{\"correctOption\":\"A\"}");
        question.setSource(QuestionSourceEnum.MANUAL);
        question.setUsageCount(usageCount);
        question.setUpdatedAt(LocalDateTime.now());
        return question;
    }

    private static AiQuestionGenerationResponse aiQuestion() {
        return new AiQuestionGenerationResponse(QuestionTypeEnum.SINGLE_CHOICE, DifficultyEnum.MEDIUM,
                "AI 题干", "{\"options\":[\"A\",\"B\"]}", "{\"correctOption\":\"B\"}", "AI 解析");
    }

    private static Paper paper(Long id, Long ownerUserId) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setOwnerUserId(ownerUserId);
        paper.setTitle("期中卷");
        paper.setGrade("三年级");
        paper.setPublisher("人教版");
        paper.setSubject("CHINESE");
        paper.setVolume("上册");
        paper.setUnit("整册");
        paper.setChapter("全部章节");
        paper.setScopeType(PaperScopeTypeEnum.VOLUME);
        paper.setTotalScore(new BigDecimal("10"));
        paper.setStatus(PaperStatusEnum.DRAFT);
        paper.setCreatedAt(LocalDateTime.now());
        paper.setUpdatedAt(LocalDateTime.now());
        return paper;
    }

    private static PaperSection section(Long id) {
        PaperSection section = new PaperSection();
        section.setId(id);
        section.setPaperId(70L);
        section.setTitle("一、选择题");
        section.setQuestionType(QuestionTypeEnum.SINGLE_CHOICE);
        section.setQuestionCount(5);
        section.setScorePerQuestion(new BigDecimal("2"));
        section.setSubtotalScore(new BigDecimal("10"));
        section.setSortOrder(1);
        return section;
    }

    private static PaperQuestion paperQuestion(Long id) {
        PaperQuestion question = new PaperQuestion();
        question.setId(id);
        question.setPaperId(70L);
        question.setSectionId(700L);
        question.setSourceQuestionId(31L);
        question.setSource(QuestionSourceEnum.MANUAL);
        question.setStemSnapshot("题干");
        question.setContentSnapshotJson("{\"options\":[\"A\",\"B\"]}");
        question.setAnswerSnapshotJson("{\"correctOption\":\"A\"}");
        question.setAnalysisSnapshot("解析");
        question.setScore(new BigDecimal("2"));
        question.setSortOrder(1);
        return question;
    }
}
