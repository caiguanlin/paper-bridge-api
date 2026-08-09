package com.paper.teacher.modules.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.template.dto.QuestionTypeTemplateRequest;
import com.paper.teacher.modules.template.dto.QuestionTypeTemplateResponse;
import com.paper.teacher.modules.template.entity.QuestionTypeTemplate;
import com.paper.teacher.modules.template.entity.QuestionTypeTemplateItem;
import com.paper.teacher.modules.template.repository.QuestionTypeTemplateItemRepository;
import com.paper.teacher.modules.template.repository.QuestionTypeTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionTypeTemplateServiceTest {
    private static final QuestionTypeTemplateRequest REQUEST = new QuestionTypeTemplateRequest("期中模板", List.of(
            new QuestionTypeTemplateRequest.ItemRequest("一、选择题", QuestionTypeEnum.SINGLE_CHOICE, 5, new BigDecimal("2")),
            new QuestionTypeTemplateRequest.ItemRequest("二、判断题", QuestionTypeEnum.TRUE_FALSE, 4, new BigDecimal("1.5"))));

    @Mock
    private QuestionTypeTemplateRepository templateRepository;
    @Mock
    private QuestionTypeTemplateItemRepository itemRepository;
    @InjectMocks
    private QuestionTypeTemplateService service;

    @Test
    void listReturnsTemplatesWithItems() {
        when(templateRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(template(1L, "期中模板")));
        when(itemRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item(11L, 1L)));

        List<QuestionTypeTemplateResponse> responses = service.list();

        assertEquals(1, responses.size());
        assertEquals("期中模板", responses.getFirst().name());
        assertEquals(1, responses.getFirst().items().size());
        assertEquals(11L, responses.getFirst().items().getFirst().id());
    }

    @Test
    void detailRejectsUnknownTemplate() {
        when(templateRepository.selectById(3L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.detail(3L));

        assertEquals("题型配置模板不存在", exception.getMessage());
    }

    @Test
    void createComputesTotalScoreAndSequentialItemSortOrder() {
        when(templateRepository.insert(any(QuestionTypeTemplate.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, QuestionTypeTemplate.class).setId(1L);
            return 1;
        });
        when(templateRepository.selectById(1L)).thenReturn(template(1L, "期中模板"));
        when(itemRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.create(REQUEST);

        ArgumentCaptor<QuestionTypeTemplate> templateCaptor = ArgumentCaptor.forClass(QuestionTypeTemplate.class);
        verify(templateRepository).insert(templateCaptor.capture());
        assertEquals(new BigDecimal("16.0"), templateCaptor.getValue().getTotalScore());
        assertEquals(0, templateCaptor.getValue().getSortOrder());

        ArgumentCaptor<QuestionTypeTemplateItem> itemCaptor = ArgumentCaptor.forClass(QuestionTypeTemplateItem.class);
        verify(itemRepository, times(2)).insert(itemCaptor.capture());
        assertEquals(1, itemCaptor.getAllValues().getFirst().getSortOrder());
        assertEquals(2, itemCaptor.getAllValues().get(1).getSortOrder());
        assertEquals(1L, itemCaptor.getAllValues().getFirst().getTemplateId());
        assertEquals(QuestionTypeEnum.SINGLE_CHOICE, itemCaptor.getAllValues().getFirst().getQuestionType());
    }

    @Test
    void updateReplacesItemsAndRefreshesTotalScore() {
        QuestionTypeTemplate existing = template(1L, "旧模板");
        when(templateRepository.selectById(1L)).thenReturn(existing);
        when(itemRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.update(1L, REQUEST);

        verify(itemRepository).delete(any(LambdaUpdateWrapper.class));
        verify(itemRepository, times(2)).insert(any(QuestionTypeTemplateItem.class));
        verify(templateRepository).updateById(existing);
        assertEquals("期中模板", existing.getName());
        assertEquals(new BigDecimal("16.0"), existing.getTotalScore());
    }

    @Test
    void updateRejectsUnknownTemplate() {
        when(templateRepository.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.update(1L, REQUEST));
        verify(itemRepository, never()).insert(any(QuestionTypeTemplateItem.class));
    }

    @Test
    void deleteRemovesTemplateItems() {
        when(templateRepository.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(itemRepository).delete(any(LambdaUpdateWrapper.class));
    }

    @Test
    void deleteRejectsUnknownTemplate() {
        when(templateRepository.deleteById(1L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(1L));

        assertEquals("题型配置模板不存在", exception.getMessage());
        verify(itemRepository, never()).delete(any(LambdaUpdateWrapper.class));
    }

    private static QuestionTypeTemplate template(Long id, String name) {
        QuestionTypeTemplate template = new QuestionTypeTemplate();
        template.setId(id);
        template.setName(name);
        template.setTotalScore(new BigDecimal("16.0"));
        template.setSortOrder(0);
        template.setUpdatedAt(LocalDateTime.now());
        return template;
    }

    private static QuestionTypeTemplateItem item(Long id, Long templateId) {
        QuestionTypeTemplateItem item = new QuestionTypeTemplateItem();
        item.setId(id);
        item.setTemplateId(templateId);
        item.setTitle("一、选择题");
        item.setQuestionType(QuestionTypeEnum.SINGLE_CHOICE);
        item.setQuestionCount(5);
        item.setScorePerQuestion(new BigDecimal("2"));
        item.setSortOrder(1);
        return item;
    }
}
