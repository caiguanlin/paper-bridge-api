package com.paper.teacher.modules.paper.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.PaperScopeTypeEnum;
import com.paper.teacher.constant.enums.PaperStatusEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.modules.paper.entity.Paper;
import com.paper.teacher.modules.paper.entity.PaperQuestion;
import com.paper.teacher.modules.paper.entity.PaperSection;
import com.paper.teacher.modules.paper.repository.PaperQuestionRepository;
import com.paper.teacher.modules.paper.repository.PaperRepository;
import com.paper.teacher.modules.paper.repository.PaperSectionRepository;
import com.paper.teacher.modules.question.entity.Question;
import com.paper.teacher.modules.question.repository.QuestionRepository;
import com.paper.teacher.modules.question.service.QuestionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperEditServiceTest {
    private static final long OWNER_ID = 4L;

    @Mock
    private PaperGenerationService paperGenerationService;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private PaperQuestionRepository paperQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionValidator questionValidator;
    @InjectMocks
    private PaperEditService paperEditService;

    @Test
    void updateQuestionRecalculatesSectionSubtotalAndPaperTotal() {
        Paper paper = paper();
        PaperQuestion question = paperQuestion(7000L, QuestionSourceEnum.MANUAL);
        PaperSection section = section();
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper);
        when(paperQuestionRepository.selectById(7000L)).thenReturn(question);
        when(paperSectionRepository.selectById(700L)).thenReturn(section);
        when(paperQuestionRepository.selectList(any(Wrapper.class)))
                .thenReturn(List.of(scored("3"), scored("4.5")));
        when(paperSectionRepository.selectList(any(Wrapper.class))).thenReturn(List.of(section));

        paperEditService.updateQuestion(OWNER_ID, 70L, 7000L, new PaperQuestionUpdateRequest(
                "新题干", "{\"options\":[\"A\",\"B\"]}", "{\"correctOption\":\"B\"}", "新解析", new BigDecimal("4.5")));

        verify(questionValidator).validate(QuestionTypeEnum.SINGLE_CHOICE,
                "{\"options\":[\"A\",\"B\"]}", "{\"correctOption\":\"B\"}");
        assertEquals("新题干", question.getStemSnapshot());
        assertEquals("新解析", question.getAnalysisSnapshot());
        assertEquals(new BigDecimal("4.5"), question.getScore());
        verify(paperQuestionRepository).updateById(question);
        assertEquals(new BigDecimal("7.5"), section.getSubtotalScore());
        verify(paperSectionRepository).updateById(section);
        assertEquals(new BigDecimal("7.5"), paper.getTotalScore());
        assertNotNull(paper.getUpdatedAt());
        verify(paperRepository).updateById(paper);
    }

    @Test
    void updateQuestionRejectsQuestionFromAnotherPaper() {
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper());
        PaperQuestion foreign = paperQuestion(7000L, QuestionSourceEnum.MANUAL);
        foreign.setPaperId(99L);
        when(paperQuestionRepository.selectById(7000L)).thenReturn(foreign);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> paperEditService.updateQuestion(OWNER_ID, 70L, 7000L, new PaperQuestionUpdateRequest(
                        "题干", "{}", "{}", null, new BigDecimal("2"))));

        assertEquals("试卷题目不存在", exception.getMessage());
        verify(paperQuestionRepository, never()).updateById(any(PaperQuestion.class));
    }

    @Test
    void updateQuestionRejectsMissingQuestion() {
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper());
        when(paperQuestionRepository.selectById(7000L)).thenReturn(null);

        assertEquals("试卷题目不存在", assertThrows(BusinessException.class,
                () -> paperEditService.updateQuestion(OWNER_ID, 70L, 7000L, new PaperQuestionUpdateRequest(
                        "题干", "{}", "{}", null, new BigDecimal("2")))).getMessage());
    }

    @Test
    void saveToBankCopiesSnapshotIntoBankWithPaperMetadata() {
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper());
        when(paperQuestionRepository.selectById(7000L)).thenReturn(paperQuestion(7000L, QuestionSourceEnum.MANUAL));
        when(paperSectionRepository.selectById(700L)).thenReturn(section());

        Question question = paperEditService.saveToBank(OWNER_ID, 70L, 7000L);

        verify(questionRepository).insert(question);
        assertEquals(OWNER_ID, question.getOwnerUserId());
        assertEquals("三年级", question.getGrade());
        assertEquals("第一单元", question.getUnit());
        assertEquals(QuestionTypeEnum.SINGLE_CHOICE, question.getQuestionType());
        assertEquals(DifficultyEnum.MEDIUM, question.getDifficulty());
        assertEquals("题干", question.getStem());
        assertEquals(QuestionSourceEnum.MANUAL, question.getSource());
        assertEquals(0, question.getUsageCount());
    }

    @Test
    void saveToBankKeepsAiSourceForAiSnapshots() {
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper());
        when(paperQuestionRepository.selectById(7000L)).thenReturn(paperQuestion(7000L, QuestionSourceEnum.AI));
        when(paperSectionRepository.selectById(700L)).thenReturn(section());

        assertEquals(QuestionSourceEnum.AI, paperEditService.saveToBank(OWNER_ID, 70L, 7000L).getSource());
    }

    @Test
    void saveToBankNormalizesImportedSourceToManual() {
        when(paperGenerationService.requirePaper(OWNER_ID, 70L)).thenReturn(paper());
        when(paperQuestionRepository.selectById(7000L))
                .thenReturn(paperQuestion(7000L, QuestionSourceEnum.EXCEL_IMPORT));
        when(paperSectionRepository.selectById(700L)).thenReturn(section());

        assertEquals(QuestionSourceEnum.MANUAL, paperEditService.saveToBank(OWNER_ID, 70L, 7000L).getSource());
    }

    private static Paper paper() {
        Paper paper = new Paper();
        paper.setId(70L);
        paper.setOwnerUserId(OWNER_ID);
        paper.setTitle("期中卷");
        paper.setGrade("三年级");
        paper.setPublisher("人教版");
        paper.setSubject("CHINESE");
        paper.setVolume("上册");
        paper.setUnit("第一单元");
        paper.setChapter("第一课");
        paper.setScopeType(PaperScopeTypeEnum.CHAPTERS);
        paper.setTotalScore(new BigDecimal("10"));
        paper.setStatus(PaperStatusEnum.DRAFT);
        paper.setUpdatedAt(LocalDateTime.now());
        return paper;
    }

    private static PaperSection section() {
        PaperSection section = new PaperSection();
        section.setId(700L);
        section.setPaperId(70L);
        section.setTitle("一、选择题");
        section.setQuestionType(QuestionTypeEnum.SINGLE_CHOICE);
        section.setQuestionCount(5);
        section.setScorePerQuestion(new BigDecimal("2"));
        section.setSubtotalScore(new BigDecimal("10"));
        section.setSortOrder(1);
        return section;
    }

    private static PaperQuestion paperQuestion(Long id, QuestionSourceEnum source) {
        PaperQuestion question = new PaperQuestion();
        question.setId(id);
        question.setPaperId(70L);
        question.setSectionId(700L);
        question.setSource(source);
        question.setStemSnapshot("题干");
        question.setContentSnapshotJson("{\"options\":[\"A\",\"B\"]}");
        question.setAnswerSnapshotJson("{\"correctOption\":\"A\"}");
        question.setAnalysisSnapshot("解析");
        question.setScore(new BigDecimal("2"));
        question.setSortOrder(1);
        return question;
    }

    private static PaperQuestion scored(String score) {
        PaperQuestion question = new PaperQuestion();
        question.setScore(new BigDecimal(score));
        return question;
    }
}
