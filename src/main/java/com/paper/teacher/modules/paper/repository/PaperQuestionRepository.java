package com.paper.teacher.modules.paper.repository;

import com.paper.teacher.modules.paper.entity.PaperQuestion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PaperQuestionRepository extends BaseMapper<PaperQuestion> {
    default List<PaperQuestion> findBySectionOrdered(Long sectionId) {
        return selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getSectionId, sectionId)
                .orderByAsc(PaperQuestion::getSortOrder));
    }

    default List<PaperQuestion> findBySection(Long sectionId) {
        return selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getSectionId, sectionId));
    }

    default void deleteByPaper(Long paperId) {
        delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId));
    }
}
