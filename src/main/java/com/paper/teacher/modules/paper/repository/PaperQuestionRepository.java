package com.paper.teacher.modules.paper.repository;

import com.paper.teacher.modules.paper.entity.PaperQuestion;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaperQuestionRepository extends BaseMapper<PaperQuestion> {
}
