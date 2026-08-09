package com.paper.teacher.modules.question.service;

import com.paper.teacher.modules.question.repository.QuestionRepository;

import com.paper.teacher.modules.question.entity.Question;

import com.paper.teacher.constant.enums.QuestionSourceEnum;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.dto.QuestionResponse;
import com.paper.teacher.modules.question.dto.QuestionSearchRequest;
import com.paper.teacher.modules.question.support.Questions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final QuestionValidator questionValidator;

    @Transactional
    public Question create(Long ownerUserId, QuestionCreateRequest request, QuestionSourceEnum source) {
        questionValidator.validate(request.questionType(), request.contentJson(), request.answerJson());
        Question question = Questions.from(ownerUserId, request, source, LocalDateTime.now());
        questionRepository.insert(question);
        return question;
    }

    public List<QuestionResponse> search(Long ownerUserId, QuestionSearchRequest request) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(Question::getOwnerUserId, ownerUserId)
                .eq(StrUtil.isNotBlank(request.grade()), Question::getGrade, request.grade())
                .eq(StrUtil.isNotBlank(request.publisher()), Question::getPublisher, request.publisher())
                .eq(StrUtil.isNotBlank(request.subject()), Question::getSubject, request.subject())
                .eq(StrUtil.isNotBlank(request.volume()), Question::getVolume, request.volume())
                .eq(StrUtil.isNotBlank(request.unit()), Question::getUnit, request.unit())
                .eq(StrUtil.isNotBlank(request.chapter()), Question::getChapter, request.chapter())
                .eq(request.questionType() != null, Question::getQuestionType, request.questionType())
                .eq(request.difficulty() != null, Question::getDifficulty, request.difficulty())
                .orderByDesc(Question::getUpdatedAt);
        return questionRepository.selectList(query).stream().map(QuestionResponse::from).toList();
    }

    public QuestionRepository repository() {
        return questionRepository;
    }
}
