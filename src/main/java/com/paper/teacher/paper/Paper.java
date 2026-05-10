package com.paper.teacher.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("paper")
@Getter
@Setter
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private String title;
    private String grade;
    private String publisher;
    private String subject;
    private String volume;
    @TableField("unit_name")
    private String unit;
    @TableField("chapter_name")
    private String chapter;
    private PaperScopeType scopeType;
    private String scopePayloadJson;
    private BigDecimal totalScore;
    private PaperStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
