package com.paper.teacher.paper;

import com.paper.teacher.modules.paper.*;
import com.paper.teacher.modules.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.modules.question.Question;
import com.paper.teacher.modules.question.QuestionRepository;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.question.QuestionValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperSnapshotIsolationTest {
    @Test
    void editingPaperQuestionDoesNotUpdateSourceQuestionBank() {
        PaperGenerationService generationService = mock(PaperGenerationService.class);
        PaperRepository paperRepository = mock(PaperRepository.class);
        PaperSectionRepository sectionRepository = mock(PaperSectionRepository.class);
        PaperQuestionRepository paperQuestionRepository = mock(PaperQuestionRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        QuestionValidator questionValidator = mock(QuestionValidator.class);
        PaperEditService service = new PaperEditService(
                generationService, paperRepository, sectionRepository, paperQuestionRepository, questionRepository, questionValidator
        );

        Paper paper = new Paper();
        paper.setId(1L);
        paper.setOwnerUserId(1L);
        when(generationService.requirePaper(1L, 1L)).thenReturn(paper);

        PaperSection section = new PaperSection();
        section.setId(2L);
        section.setQuestionType(QuestionTypeEnum.TRUE_FALSE);
        when(sectionRepository.selectById(2L)).thenReturn(section);

        PaperQuestion snapshot = new PaperQuestion();
        snapshot.setId(3L);
        snapshot.setPaperId(1L);
        snapshot.setSectionId(2L);
        snapshot.setSourceQuestionId(99L);
        snapshot.setScore(BigDecimal.TEN);
        when(paperQuestionRepository.selectById(3L)).thenReturn(snapshot);
        when(paperQuestionRepository.selectList(any())).thenReturn(List.of(snapshot));
        when(sectionRepository.selectList(any())).thenReturn(List.of(section));

        service.updateQuestion(1L, 1L, 3L, new PaperQuestionUpdateRequest(
                "修改后的题干",
                "{\"statement\":\"修改后的题干\"}",
                "{\"correctBoolean\":false}",
                "修改后的解析",
                BigDecimal.TEN
        ));

        verify(paperQuestionRepository).updateById(snapshot);
        verify(questionRepository, never()).updateById(any(Question.class));
    }
}
