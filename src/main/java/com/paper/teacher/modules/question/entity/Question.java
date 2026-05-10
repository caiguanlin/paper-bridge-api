package com.paper.teacher.modules.question.entity;

import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("question")
@Getter
@Setter
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private String grade;
    private String publisher;
    private String subject;
    private String volume;
    @TableField("unit_name")
    private String unit;
    @TableField("chapter_name")
    private String chapter;
    private QuestionTypeEnum questionType;
    private DifficultyEnum difficulty;
    private String stem;
    private String contentJson;
    private String answerJson;
    private String analysis;
    private QuestionSourceEnum source;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
