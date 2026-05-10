package com.paper.teacher.template;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.question.QuestionType;
import com.paper.teacher.template.dto.QuestionTypeTemplateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionTypeTemplateServiceTest {
    private final QuestionTypeTemplateRepository templateRepository = mock(QuestionTypeTemplateRepository.class);
    private final QuestionTypeTemplateItemRepository itemRepository = mock(QuestionTypeTemplateItemRepository.class);
    private final QuestionTypeTemplateService service = new QuestionTypeTemplateService(templateRepository, itemRepository);

    @Test
    void listReturnsTemplatesWithItems() {
        QuestionTypeTemplate template = template(1L);
        when(templateRepository.selectList(any(Wrapper.class))).thenReturn(List.of(template));
        when(itemRepository.selectList(any(Wrapper.class))).thenReturn(List.of(item(11L)));

        var templates = service.list();

        assertThat(templates).singleElement().satisfies(response -> {
            assertThat(response.name()).isEqualTo("100分基础模板");
            assertThat(response.items()).singleElement()
                    .satisfies(item -> assertThat(item.questionType()).isEqualTo(QuestionType.SINGLE_CHOICE));
        });
    }

    @Test
    void updateRejectsMissingTemplate() {
        when(templateRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(99L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("题型配置模板不存在");
    }

    @Test
    void createStoresTemplateAndItems() {
        QuestionTypeTemplate saved = template(1L);
        doAnswer(invocation -> {
            QuestionTypeTemplate template = invocation.getArgument(0);
            assertThat(template.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(template.getSortOrder()).isZero();
            template.setId(1L);
            return 1;
        }).when(templateRepository).insert(any(QuestionTypeTemplate.class));
        when(templateRepository.selectById(1L)).thenReturn(saved);
        when(itemRepository.selectList(any(Wrapper.class))).thenReturn(List.of(item(11L)));

        var response = service.create(request());

        assertThat(response.id()).isEqualTo(1L);
        verify(itemRepository, times(2)).insert(any(QuestionTypeTemplateItem.class));
    }

    private QuestionTypeTemplateRequest request() {
        return new QuestionTypeTemplateRequest(
                "100分基础模板",
                List.of(
                        new QuestionTypeTemplateRequest.ItemRequest(
                                "选择题",
                                QuestionType.SINGLE_CHOICE,
                                10,
                                BigDecimal.valueOf(5)
                        ),
                        new QuestionTypeTemplateRequest.ItemRequest(
                                "填空题",
                                QuestionType.FILL_BLANK,
                                10,
                                BigDecimal.valueOf(5)
                        )
                )
        );
    }

    private QuestionTypeTemplate template(Long id) {
        QuestionTypeTemplate template = new QuestionTypeTemplate();
        template.setId(id);
        template.setName("100分基础模板");
        template.setTotalScore(BigDecimal.valueOf(100));
        template.setSortOrder(1);
        return template;
    }

    private QuestionTypeTemplateItem item(Long id) {
        QuestionTypeTemplateItem item = new QuestionTypeTemplateItem();
        item.setId(id);
        item.setTemplateId(1L);
        item.setTitle("选择题");
        item.setQuestionType(QuestionType.SINGLE_CHOICE);
        item.setQuestionCount(10);
        item.setScorePerQuestion(BigDecimal.valueOf(5));
        item.setSortOrder(1);
        return item;
    }
}
