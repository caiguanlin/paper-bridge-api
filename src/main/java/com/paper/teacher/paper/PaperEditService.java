package com.paper.teacher.paper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.question.Question;
import com.paper.teacher.question.QuestionRepository;
import com.paper.teacher.question.QuestionSource;
import com.paper.teacher.question.QuestionValidator;
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

        LocalDateTime now = LocalDateTime.now();
        Question question = new Question();
        question.setOwnerUserId(ownerUserId);
        question.setGrade(paper.getGrade());
        question.setPublisher(paper.getPublisher());
        question.setSubject(paper.getSubject());
        question.setVolume(paper.getVolume());
        question.setUnit(paper.getUnit());
        question.setChapter(paper.getChapter());
        question.setQuestionType(section.getQuestionType());
        question.setDifficulty(com.paper.teacher.question.Difficulty.MEDIUM);
        question.setStem(snapshot.getStemSnapshot());
        question.setContentJson(snapshot.getContentSnapshotJson());
        question.setAnswerJson(snapshot.getAnswerSnapshotJson());
        question.setAnalysis(snapshot.getAnalysisSnapshot());
        question.setSource(snapshot.getSource() == QuestionSource.AI ? QuestionSource.AI : QuestionSource.MANUAL);
        question.setUsageCount(0);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        questionRepository.insert(question);
        return question;
    }

    private PaperQuestion requirePaperQuestion(Long paperId, Long paperQuestionId) {
        PaperQuestion question = paperQuestionRepository.selectById(paperQuestionId);
        if (question == null || !question.getPaperId().equals(paperId)) {
            throw new BusinessException("试卷题目不存在");
        }
        return question;
    }

    private void recalculateSection(PaperSection section) {
        BigDecimal subtotal = paperQuestionRepository.selectList(new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getSectionId, section.getId()))
                .stream()
                .map(PaperQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        section.setSubtotalScore(subtotal);
        paperSectionRepository.updateById(section);
    }

    private BigDecimal totalScore(Long paperId) {
        return paperSectionRepository.selectList(new LambdaQueryWrapper<PaperSection>()
                        .eq(PaperSection::getPaperId, paperId))
                .stream()
                .map(PaperSection::getSubtotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
