package com.paper.teacher.modules.paper.service;

import com.paper.teacher.modules.paper.repository.PaperSectionRepository;

import com.paper.teacher.modules.paper.repository.PaperRepository;

import com.paper.teacher.modules.paper.repository.PaperQuestionRepository;

import com.paper.teacher.modules.paper.entity.PaperSection;

import com.paper.teacher.modules.paper.entity.PaperQuestion;

import com.paper.teacher.modules.paper.entity.Paper;

import com.paper.teacher.common.Entities;
import com.paper.teacher.common.Scores;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.modules.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.entity.Question;
import com.paper.teacher.modules.question.repository.QuestionRepository;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.modules.question.service.QuestionValidator;
import com.paper.teacher.modules.question.support.Questions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaperEditService {
    private final PaperGenerationService paperGenerationService;
    private final PaperRepository paperRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionValidator questionValidator;

    @Transactional
    public void updateQuestion(Long ownerUserId, Long paperId, Long paperQuestionId, PaperQuestionUpdateRequest request) {
        Paper paper = paperGenerationService.requirePaper(ownerUserId, paperId);
        PaperQuestion question = requirePaperQuestion(paperId, paperQuestionId);
        PaperSection section = paperSectionRepository.selectById(question.getSectionId());
        questionValidator.validate(section.getQuestionType(), request.contentSnapshotJson(), request.answerSnapshotJson());
        question.setStemSnapshot(request.stemSnapshot());
        question.setContentSnapshotJson(request.contentSnapshotJson());
        question.setAnswerSnapshotJson(request.answerSnapshotJson());
        question.setAnalysisSnapshot(request.analysisSnapshot());
        question.setScore(request.score());
        paperQuestionRepository.updateById(question);
        recalculateSection(section);
        paper.setTotalScore(totalScore(paperId));
        paper.setUpdatedAt(LocalDateTime.now());
        paperRepository.updateById(paper);
    }

    @Transactional
    public Question saveToBank(Long ownerUserId, Long paperId, Long paperQuestionId) {
        Paper paper = paperGenerationService.requirePaper(ownerUserId, paperId);
        PaperQuestion snapshot = requirePaperQuestion(paperId, paperQuestionId);
        PaperSection section = paperSectionRepository.selectById(snapshot.getSectionId());
        questionValidator.validate(section.getQuestionType(), snapshot.getContentSnapshotJson(), snapshot.getAnswerSnapshotJson());

        QuestionCreateRequest bankRequest = new QuestionCreateRequest(
                paper.getGrade(),
                paper.getPublisher(),
                paper.getSubject(),
                paper.getVolume(),
                paper.getUnit(),
                paper.getChapter(),
                section.getQuestionType(),
                DifficultyEnum.MEDIUM,
                snapshot.getStemSnapshot(),
                snapshot.getContentSnapshotJson(),
                snapshot.getAnswerSnapshotJson(),
                snapshot.getAnalysisSnapshot()
        );
        QuestionSourceEnum source = snapshot.getSource() == QuestionSourceEnum.AI
                ? QuestionSourceEnum.AI
                : QuestionSourceEnum.MANUAL;
        Question question = Questions.from(ownerUserId, bankRequest, source, LocalDateTime.now());
        questionRepository.insert(question);
        return question;
    }

    private PaperQuestion requirePaperQuestion(Long paperId, Long paperQuestionId) {
        PaperQuestion question = Entities.require(
                paperQuestionRepository.selectById(paperQuestionId), "试卷题目不存在");
        Entities.check(question.getPaperId().equals(paperId), "试卷题目不存在");
        return question;
    }

    private void recalculateSection(PaperSection section) {
        section.setSubtotalScore(Scores.sumOf(
                paperQuestionRepository.findBySection(section.getId()), PaperQuestion::getScore));
        paperSectionRepository.updateById(section);
    }

    private BigDecimal totalScore(Long paperId) {
        return Scores.sumOf(paperSectionRepository.findByPaper(paperId), PaperSection::getSubtotalScore);
    }
}
