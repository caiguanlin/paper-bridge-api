package com.paper.teacher.modules.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.dto.QuestionResponse;
import com.paper.teacher.modules.question.dto.QuestionSearchRequest;
import com.paper.teacher.modules.question.entity.Question;
import com.paper.teacher.modules.question.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionValidator questionValidator;
    @InjectMocks
    private QuestionService questionService;

    @Test
    void createPersistsQuestionWithSourceAndTimestamps() {
        QuestionCreateRequest request = new QuestionCreateRequest(
                "三年级", "人教版", "CHINESE", "上册", "第一单元", "第一课",
                QuestionTypeEnum.DICTATION, DifficultyEnum.EASY, "默写静夜思",
                "{\"prompt\":\"默写静夜思\"}", "{\"expectedText\":\"床前明月光\"}", "考查古诗背诵");

        Question created = questionService.create(9L, request, QuestionSourceEnum.EXCEL_IMPORT);

        verify(questionValidator).validate(QuestionTypeEnum.DICTATION, request.contentJson(), request.answerJson());
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).insert(captor.capture());
        Question inserted = captor.getValue();
        assertSame(created, inserted);
        assertEquals(9L, inserted.getOwnerUserId());
        assertEquals("第一单元", inserted.getUnit());
        assertEquals("第一课", inserted.getChapter());
        assertEquals(QuestionSourceEnum.EXCEL_IMPORT, inserted.getSource());
        assertEquals(0, inserted.getUsageCount());
        assertNotNull(inserted.getCreatedAt());
        assertEquals(inserted.getCreatedAt(), inserted.getUpdatedAt());
    }

    @Test
    void createDoesNotInsertWhenValidationFails() {
        QuestionCreateRequest request = new QuestionCreateRequest(
                "三年级", "人教版", "CHINESE", "上册", "第一单元", "第一课",
                QuestionTypeEnum.SINGLE_CHOICE, DifficultyEnum.EASY, "题干",
                "{}", "{}", null);
        doThrow(new BusinessException("选择题至少需要两个选项"))
                .when(questionValidator).validate(any(), any(), any());

        assertThrows(BusinessException.class,
                () -> questionService.create(1L, request, QuestionSourceEnum.MANUAL));
        verify(questionRepository, never()).insert(any(Question.class));
    }

    @Test
    void searchMapsRepositoryResultsToResponses() {
        Question question = new Question();
        question.setId(3L);
        question.setStem("题干");
        when(questionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(question));

        List<QuestionResponse> responses = questionService.search(
                1L, new QuestionSearchRequest("三年级", "人教版", null, null, null, null, null, null));

        assertEquals(1, responses.size());
        assertEquals(3L, responses.getFirst().id());
        assertEquals("题干", responses.getFirst().stem());
    }

    @Test
    void repositoryExposesUnderlyingMapper() {
        assertSame(questionRepository, questionService.repository());
    }
}
