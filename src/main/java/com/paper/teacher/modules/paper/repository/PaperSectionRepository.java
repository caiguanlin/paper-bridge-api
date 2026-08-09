package com.paper.teacher.modules.paper.repository;

import com.paper.teacher.modules.paper.entity.PaperSection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PaperSectionRepository extends BaseMapper<PaperSection> {
    default List<PaperSection> findByPaperOrdered(Long paperId) {
        return selectList(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId)
                .orderByAsc(PaperSection::getSortOrder));
    }

    default List<PaperSection> findByPaper(Long paperId) {
        return selectList(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId));
    }

    default void deleteByPaper(Long paperId) {
        delete(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId));
    }
}
