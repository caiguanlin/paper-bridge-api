package com.paper.teacher.template;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.teacher.question.QuestionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@TableName("question_type_template_item")
@Getter
@Setter
public class QuestionTypeTemplateItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String title;
    private QuestionType questionType;
    private Integer questionCount;
    private BigDecimal scorePerQuestion;
    private Integer sortOrder;
}
