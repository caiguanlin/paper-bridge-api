package com.paper.teacher.modules.template.service;

import com.paper.teacher.modules.template.repository.QuestionTypeTemplateRepository;

import com.paper.teacher.modules.template.repository.QuestionTypeTemplateItemRepository;

import com.paper.teacher.modules.template.entity.QuestionTypeTemplateItem;

import com.paper.teacher.modules.template.entity.QuestionTypeTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.Entities;
import com.paper.teacher.common.Scores;
import com.paper.teacher.modules.template.dto.QuestionTypeTemplateRequest;
import com.paper.teacher.modules.template.dto.QuestionTypeTemplateResponse;
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
        LocalDateTime now = LocalDateTime.now();
        QuestionTypeTemplate template = new QuestionTypeTemplate();
        template.setName(request.name());
        template.setTotalScore(calculateTotalScore(request.items()));
        template.setSortOrder(0);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        templateRepository.insert(template);
        insertItems(template.getId(), request.items());
        return detail(template.getId());
    }

    @Transactional
    public QuestionTypeTemplateResponse update(Long id, QuestionTypeTemplateRequest request) {
        QuestionTypeTemplate template = findTemplate(id);
        template.setName(request.name());
        template.setTotalScore(calculateTotalScore(request.items()));
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.updateById(template);
        itemRepository.deleteByTemplate(id);
        insertItems(id, request.items());
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        Entities.requireAffected(templateRepository.deleteById(id), "题型配置模板不存在");
        itemRepository.deleteByTemplate(id);
    }

    private QuestionTypeTemplate findTemplate(Long id) {
        return Entities.require(templateRepository.selectById(id), "题型配置模板不存在");
    }

    private List<QuestionTypeTemplateItem> items(Long templateId) {
        return itemRepository.findByTemplateOrdered(templateId);
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

    private BigDecimal calculateTotalScore(List<QuestionTypeTemplateRequest.ItemRequest> items) {
        return Scores.sumOf(items, QuestionTypeTemplateRequest.ItemRequest::subtotal);
    }
}
