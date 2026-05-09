package com.paper.teacher.paper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.ai.AiQuestionClient;
import com.paper.teacher.ai.AiQuestionGenerationRequest;
import com.paper.teacher.ai.AiQuestionGenerationResponse;
import com.paper.teacher.ai.AiQuestionValidator;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.paper.dto.PaperGenerateRequest;
import com.paper.teacher.paper.dto.PaperPlanPreview;
import com.paper.teacher.paper.dto.PaperResponse;
import com.paper.teacher.paper.dto.PaperSummaryResponse;
import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.Question;
import com.paper.teacher.question.QuestionRepository;
import com.paper.teacher.question.QuestionSource;
import com.paper.teacher.question.QuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperGenerationService {
    private final PaperRepository paperRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AiQuestionClient aiQuestionClient;
    private final AiQuestionValidator aiQuestionValidator;

    public PaperPlanPreview preview(Long ownerUserId, PaperGenerateRequest request) {
        validateScore(request);
        List<PaperPlanPreview.SectionPreview> sections = new ArrayList<>();
        for (PaperGenerateRequest.SectionRequest section : request.sections()) {
            int available = request.strategy() == GenerationStrategy.AI_ONLY
                    ? 0
                    : countAvailable(ownerUserId, request, section.questionType(), request.difficulty());
            int supplement = Math.max(0, section.questionCount() - available);
            if (request.strategy() == GenerationStrategy.BANK_ONLY) {
                supplement = 0;
            }
            sections.add(new PaperPlanPreview.SectionPreview(
                    section.title(),
                    section.questionType(),
                    section.questionCount(),
                    available,
                    supplement,
                    section.subtotal()
            ));
        }
        return new PaperPlanPreview(request.totalScore(), subtotal(request), sections);
    }

    @Transactional
    public PaperResponse generate(Long ownerUserId, PaperGenerateRequest request) {
        validateScore(request);
        LocalDateTime now = LocalDateTime.now();
        Paper paper = new Paper();
        paper.setOwnerUserId(ownerUserId);
        paper.setTitle(request.title());
        paper.setGrade(request.grade());
        paper.setPublisher(request.publisher());
        paper.setSubject(request.subject());
        paper.setVolume(request.volume());
        paper.setUnit(request.unit());
        paper.setChapter(request.chapter());
        paper.setTotalScore(request.totalScore());
        paper.setStatus(PaperStatus.DRAFT);
        paper.setCreatedAt(now);
        paper.setUpdatedAt(now);
        paperRepository.insert(paper);

        int sectionOrder = 1;
        for (PaperGenerateRequest.SectionRequest sectionRequest : request.sections()) {
            PaperSection section = createSection(paper.getId(), sectionRequest, sectionOrder++);
            List<Question> bankQuestions = request.strategy() == GenerationStrategy.AI_ONLY
                    ? List.of()
                    : selectBankQuestions(ownerUserId, request, sectionRequest.questionType(), request.difficulty(), sectionRequest.questionCount());

            if (bankQuestions.size() < sectionRequest.questionCount() && request.strategy() == GenerationStrategy.BANK_ONLY) {
                throw new BusinessException(sectionRequest.title() + "题库数量不足，缺少 "
                        + (sectionRequest.questionCount() - bankQuestions.size()) + " 道题");
            }

            int sortOrder = 1;
            for (Question question : bankQuestions) {
                snapshotBankQuestion(paper.getId(), section.getId(), question, sectionRequest.scorePerQuestion(), sortOrder++);
                question.setUsageCount((question.getUsageCount() == null ? 0 : question.getUsageCount()) + 1);
                question.setUpdatedAt(now);
                questionRepository.updateById(question);
            }

            int missing = sectionRequest.questionCount() - bankQuestions.size();
            if (missing > 0 && request.strategy() != GenerationStrategy.BANK_ONLY) {
                for (AiQuestionGenerationResponse response : generateAiQuestions(request, sectionRequest, missing)) {
                    aiQuestionValidator.validate(response);
                    snapshotAiQuestion(paper.getId(), section.getId(), response, sectionRequest.scorePerQuestion(), sortOrder++);
                }
            }
        }

        return loadPaper(ownerUserId, paper.getId());
    }

    public List<PaperSummaryResponse> list(Long ownerUserId) {
        return paperRepository.selectList(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getOwnerUserId, ownerUserId)
                        .orderByDesc(Paper::getUpdatedAt))
                .stream()
                .map(PaperSummaryResponse::from)
                .toList();
    }

    public PaperResponse loadPaper(Long ownerUserId, Long paperId) {
        Paper paper = requirePaper(ownerUserId, paperId);
        List<PaperSection> sections = paperSectionRepository.selectList(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId)
                .orderByAsc(PaperSection::getSortOrder));
        List<PaperResponse.SectionResponse> sectionResponses = sections.stream().map(section -> {
            List<PaperResponse.QuestionResponse> questions = paperQuestionRepository.selectList(new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getSectionId, section.getId())
                            .orderByAsc(PaperQuestion::getSortOrder))
                    .stream()
                    .map(PaperResponse.QuestionResponse::from)
                    .toList();
            return PaperResponse.SectionResponse.from(section, questions);
        }).toList();
        return PaperResponse.from(paper, sectionResponses);
    }

    Paper requirePaper(Long ownerUserId, Long paperId) {
        Paper paper = paperRepository.selectById(paperId);
        if (paper == null || !paper.getOwnerUserId().equals(ownerUserId)) {
            throw new BusinessException("试卷不存在");
        }
        return paper;
    }

    private PaperSection createSection(Long paperId, PaperGenerateRequest.SectionRequest request, int sortOrder) {
        PaperSection section = new PaperSection();
        section.setPaperId(paperId);
        section.setTitle(request.title());
        section.setQuestionType(request.questionType());
        section.setQuestionCount(request.questionCount());
        section.setScorePerQuestion(request.scorePerQuestion());
        section.setSubtotalScore(request.subtotal());
        section.setSortOrder(sortOrder);
        paperSectionRepository.insert(section);
        return section;
    }

    private void snapshotBankQuestion(Long paperId, Long sectionId, Question question, BigDecimal score, int sortOrder) {
        PaperQuestion snapshot = new PaperQuestion();
        snapshot.setPaperId(paperId);
        snapshot.setSectionId(sectionId);
        snapshot.setSourceQuestionId(question.getId());
        snapshot.setSource(question.getSource());
        snapshot.setStemSnapshot(question.getStem());
        snapshot.setContentSnapshotJson(question.getContentJson());
        snapshot.setAnswerSnapshotJson(question.getAnswerJson());
        snapshot.setAnalysisSnapshot(question.getAnalysis());
        snapshot.setScore(score);
        snapshot.setSortOrder(sortOrder);
        paperQuestionRepository.insert(snapshot);
    }

    private void snapshotAiQuestion(Long paperId, Long sectionId, AiQuestionGenerationResponse response, BigDecimal score, int sortOrder) {
        PaperQuestion snapshot = new PaperQuestion();
        snapshot.setPaperId(paperId);
        snapshot.setSectionId(sectionId);
        snapshot.setSourceQuestionId(null);
        snapshot.setSource(QuestionSource.AI);
        snapshot.setStemSnapshot(response.stem());
        snapshot.setContentSnapshotJson(response.contentJson());
        snapshot.setAnswerSnapshotJson(response.answerJson());
        snapshot.setAnalysisSnapshot(response.analysis());
        snapshot.setScore(score);
        snapshot.setSortOrder(sortOrder);
        paperQuestionRepository.insert(snapshot);
    }

    private List<AiQuestionGenerationResponse> generateAiQuestions(
            PaperGenerateRequest request,
            PaperGenerateRequest.SectionRequest section,
            int missing
    ) {
        Difficulty difficulty = request.difficulty() == null ? Difficulty.MEDIUM : request.difficulty();
        return aiQuestionClient.generate(new AiQuestionGenerationRequest(
                request.grade(), request.publisher(), request.subject(), request.volume(), request.unit(), request.chapter(),
                section.questionType(), difficulty, missing, section.scorePerQuestion()
        ));
    }

    private int countAvailable(Long ownerUserId, PaperGenerateRequest request, QuestionType type, Difficulty difficulty) {
        return Math.toIntExact(questionRepository.selectCount(scopeQuery(ownerUserId, request, type, difficulty)));
    }

    private List<Question> selectBankQuestions(
            Long ownerUserId,
            PaperGenerateRequest request,
            QuestionType type,
            Difficulty difficulty,
            int limit
    ) {
        return questionRepository.selectList(scopeQuery(ownerUserId, request, type, difficulty))
                .stream()
                .sorted(Comparator.comparing(Question::getUsageCount, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(Question::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    private LambdaQueryWrapper<Question> scopeQuery(Long ownerUserId, PaperGenerateRequest request, QuestionType type, Difficulty difficulty) {
        return new LambdaQueryWrapper<Question>()
                .eq(Question::getOwnerUserId, ownerUserId)
                .eq(Question::getGrade, request.grade())
                .eq(Question::getPublisher, request.publisher())
                .eq(Question::getSubject, request.subject())
                .eq(Question::getVolume, request.volume())
                .eq(Question::getUnit, request.unit())
                .eq(Question::getChapter, request.chapter())
                .eq(Question::getQuestionType, type)
                .eq(difficulty != null, Question::getDifficulty, difficulty);
    }

    private void validateScore(PaperGenerateRequest request) {
        BigDecimal subtotal = subtotal(request);
        if (subtotal.compareTo(request.totalScore()) != 0) {
            throw new BusinessException("总分必须等于各题型小计之和，当前小计为 " + subtotal);
        }
    }

    private BigDecimal subtotal(PaperGenerateRequest request) {
        return request.sections().stream()
                .map(PaperGenerateRequest.SectionRequest::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
