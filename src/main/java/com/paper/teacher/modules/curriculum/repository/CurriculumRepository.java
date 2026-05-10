package com.paper.teacher.modules.curriculum.repository;

import com.paper.teacher.modules.curriculum.entity.CurriculumNode;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CurriculumRepository extends BaseMapper<CurriculumNode> {
}
