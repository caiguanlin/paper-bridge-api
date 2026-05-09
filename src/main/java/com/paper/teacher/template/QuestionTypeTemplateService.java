package com.paper.teacher.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.template.dto.QuestionTypeTemplateRequest;
import com.paper.teacher.template.dto.QuestionTypeTemplateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionTypeTemplateService {
    private final QuestionTypeTemplateRepository templateRepository;
    private final QuestionTypeTemplateItemRepository itemRepository;

    public List<QuestionTypeTemplateResponse> list() {
        return templateRepository.selectList(new LambdaQueryWrapper<QuestionTypeTemplate>()
                        .orderByAsc(QuestionTypeTemplate::getSortOrder)
                        .orderByDesc(QuestionTypeTemplate::getUpdatedAt))
                .stream()
                .map(template -> QuestionTypeTemplateResponse.from(template, items(template.getId())))
                .toList();
    }

    public QuestionTypeTemplateResponse detail(Long id) {
        return QuestionTypeTemplateResponse.from(findTemplate(id), items(id));
    }

    @Transactional
    public QuestionTypeTemplateResponse create(QuestionTypeTemplateRequest request) {
        validateSubtotal(request);
        LocalDateTime now = LocalDateTime.now();
        QuestionTypeTemplate template = new QuestionTypeTemplate();
        template.setName(request.name());
        template.setTotalScore(request.totalScore());
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        templateRepository.insert(template);
        insertItems(template.getId(), request.items());
        return detail(template.getId());
    }

    @Transactional
    public QuestionTypeTemplateResponse update(Long id, QuestionTypeTemplateRequest request) {
        validateSubtotal(request);
        QuestionTypeTemplate template = findTemplate(id);
        template.setName(request.name());
        template.setTotalScore(request.totalScore());
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.updateById(template);
        itemRepository.delete(new LambdaUpdateWrapper<QuestionTypeTemplateItem>()
                .eq(QuestionTypeTemplateItem::getTemplateId, id));
        insertItems(id, request.items());
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        if (templateRepository.deleteById(id) == 0) {
            throw new BusinessException("题型配置模板不存在");
        }
        itemRepository.delete(new LambdaUpdateWrapper<QuestionTypeTemplateItem>()
                .eq(QuestionTypeTemplateItem::getTemplateId, id));
    }

    private QuestionTypeTemplate findTemplate(Long id) {
        QuestionTypeTemplate template = templateRepository.selectById(id);
        if (template == null) {
            throw new BusinessException("题型配置模板不存在");
        }
        return template;
    }

    private List<QuestionTypeTemplateItem> items(Long templateId) {
        return itemRepository.selectList(new LambdaQueryWrapper<QuestionTypeTemplateItem>()
                .eq(QuestionTypeTemplateItem::getTemplateId, templateId)
                .orderByAsc(QuestionTypeTemplateItem::getSortOrder)
                .orderByAsc(QuestionTypeTemplateItem::getId));
    }

    private void insertItems(Long templateId, List<QuestionTypeTemplateRequest.ItemRequest> items) {
        for (int i = 0; i < items.size(); i++) {
            QuestionTypeTemplateRequest.ItemRequest request = items.get(i);
            QuestionTypeTemplateItem item = new QuestionTypeTemplateItem();
            item.setTemplateId(templateId);
            item.setTitle(request.title());
            item.setQuestionType(request.questionType());
            item.setQuestionCount(request.questionCount());
            item.setScorePerQuestion(request.scorePerQuestion());
            item.setSortOrder(i + 1);
            itemRepository.insert(item);
        }
    }

    private void validateSubtotal(QuestionTypeTemplateRequest request) {
        BigDecimal subtotal = request.items().stream()
                .map(QuestionTypeTemplateRequest.ItemRequest::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(request.totalScore()) != 0) {
            throw new BusinessException("题型配置小计必须等于模板总分");
        }
    }
}
