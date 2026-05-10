package com.paper.teacher.modules.template;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("question_type_template")
@Getter
@Setter
public class QuestionTypeTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private BigDecimal totalScore;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
