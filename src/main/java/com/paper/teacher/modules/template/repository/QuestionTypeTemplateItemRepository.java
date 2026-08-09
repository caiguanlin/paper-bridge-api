package com.paper.teacher.modules.template.repository;

import com.paper.teacher.modules.template.entity.QuestionTypeTemplateItem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuestionTypeTemplateItemRepository extends BaseMapper<QuestionTypeTemplateItem> {
    default List<QuestionTypeTemplateItem> findByTemplateOrdered(Long templateId) {
        return selectList(new LambdaQueryWrapper<QuestionTypeTemplateItem>()
                .eq(QuestionTypeTemplateItem::getTemplateId, templateId)
                .orderByAsc(QuestionTypeTemplateItem::getSortOrder)
                .orderByAsc(QuestionTypeTemplateItem::getId));
    }

    default void deleteByTemplate(Long templateId) {
        delete(new LambdaQueryWrapper<QuestionTypeTemplateItem>()
                .eq(QuestionTypeTemplateItem::getTemplateId, templateId));
    }
}
