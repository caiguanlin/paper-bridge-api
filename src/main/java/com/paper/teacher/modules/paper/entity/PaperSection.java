package com.paper.teacher.modules.paper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@TableName("paper_section")
@Getter
@Setter
public class PaperSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String title;
    private QuestionTypeEnum questionType;
    private Integer questionCount;
    private BigDecimal scorePerQuestion;
    private BigDecimal subtotalScore;
    private Integer sortOrder;
}
