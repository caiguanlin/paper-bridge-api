package com.paper.teacher.modules.question.repository;

import com.paper.teacher.modules.question.entity.Question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionRepository extends BaseMapper<Question> {
}
