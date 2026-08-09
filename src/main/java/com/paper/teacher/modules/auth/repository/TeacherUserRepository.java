package com.paper.teacher.modules.auth.repository;

import com.paper.teacher.modules.auth.entity.TeacherUser;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherUserRepository extends BaseMapper<TeacherUser> {
    default TeacherUser findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<TeacherUser>()
                .eq(TeacherUser::getUsername, username));
    }
}
