package com.paper.teacher.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.teacher.question.QuestionSource;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@TableName("paper_question")
@Getter
@Setter
public class PaperQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private Long sectionId;
    private Long sourceQuestionId;
    private QuestionSource source;
    private String stemSnapshot;
    private String contentSnapshotJson;
    private String answerSnapshotJson;
    private String analysisSnapshot;
    private BigDecimal score;
    private Integer sortOrder;
}
