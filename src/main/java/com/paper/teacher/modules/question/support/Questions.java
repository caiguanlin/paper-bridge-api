package com.paper.teacher.modules.question.support;

import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.entity.Question;

import java.time.LocalDateTime;

public final class Questions {
    private Questions() {
    }

    public static Question from(
            Long ownerUserId,
            QuestionCreateRequest request,
            QuestionSourceEnum source,
            LocalDateTime now
    ) {
        Question question = new Question();
        question.setOwnerUserId(ownerUserId);
        question.setGrade(request.grade());
        question.setPublisher(request.publisher());
        question.setSubject(request.subject());
        question.setVolume(request.volume());
        question.setUnit(request.unit());
        question.setChapter(request.chapter());
        question.setQuestionType(request.questionType());
        question.setDifficulty(request.difficulty());
        question.setStem(request.stem());
        question.setContentJson(request.contentJson());
        question.setAnswerJson(request.answerJson());
        question.setAnalysis(request.analysis());
        question.setSource(source);
        question.setUsageCount(0);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        return question;
    }
}
