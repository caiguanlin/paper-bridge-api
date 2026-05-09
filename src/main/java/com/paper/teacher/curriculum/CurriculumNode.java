package com.paper.teacher.curriculum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("curriculum_node")
@Getter
@Setter
public class CurriculumNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String grade;
    private String publisher;
    private String subject;
    private String volume;
    @TableField("unit_name")
    private String unit;
    @TableField("chapter_name")
    private String chapter;
    private Integer sortOrder;
    @TableField("edition_year")
    private Integer editionYear;
    @TableField("source_url")
    private String sourceUrl;
    @TableField("source_code")
    private String sourceCode;
}
