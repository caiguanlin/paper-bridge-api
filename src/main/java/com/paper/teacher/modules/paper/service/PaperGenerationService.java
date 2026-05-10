package com.paper.teacher.modules.paper.service;

import com.paper.teacher.modules.paper.repository.PaperSectionRepository;

import com.paper.teacher.modules.paper.repository.PaperRepository;

import com.paper.teacher.modules.paper.repository.PaperQuestionRepository;

import com.paper.teacher.modules.paper.entity.PaperSection;

import com.paper.teacher.modules.paper.entity.PaperQuestion;

import com.paper.teacher.modules.paper.entity.Paper;

import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.GenerationStrategyEnum;
import com.paper.teacher.constant.enums.PaperScopeTypeEnum;
import com.paper.teacher.constant.enums.PaperStatusEnum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.modules.ai.service.AiQuestionClient;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationRequest;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;
import com.paper.teacher.modules.ai.service.AiQuestionValidator;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest.ChapterScope;
import com.paper.teacher.modules.paper.dto.PaperPlanPreview;
import com.paper.teacher.modules.paper.dto.PaperResponse;
import com.paper.teacher.modules.paper.dto.PaperSummaryResponse;
import com.paper.teacher.modules.question.entity.Question;
import com.paper.teacher.modules.question.repository.QuestionRepository;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperGenerationService {
    private static final String ALL_CHAPTERS_DISPLAY = "全部章节";
    private static final String WHOLE_VOLUME_DISPLAY = "整册";

    private final PaperRepository paperRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AiQuestionClient aiQuestionClient;
    private final AiQuestionValidator aiQuestionValidator;
    private final ObjectMapper objectMapper;

    public PaperPlanPreview preview(Long ownerUserId, PaperGenerateRequest request) {
        validateScope(request);
        validateScore(request);
        List<PaperPlanPreview.SectionPreview> sections = new ArrayList<>();
        for (PaperGenerateRequest.SectionRequest section : request.sections()) {
            int available = request.strategy() == GenerationStrategyEnum.AI_ONLY
                    ? 0
                    : countAvailable(ownerUserId, request, section.questionType(), request.difficulty());
            int supplement = Math.max(0, section.questionCount() - available);
            if (request.strategy() == GenerationStrategyEnum.BANK_ONLY) {
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
        validateScope(request);
        validateScore(request);
        LocalDateTime now = LocalDateTime.now();
        ScopeDisplay display = scopeDisplay(request);
        Paper paper = new Paper();
        paper.setOwnerUserId(ownerUserId);
        paper.setTitle(request.title());
        paper.setGrade(request.grade());
        paper.setPublisher(request.publisher());
        paper.setSubject(request.subject());
        paper.setVolume(request.volume());
        paper.setUnit(display.unit());
        paper.setChapter(display.chapter());
        paper.setScopeType(request.scopeType());
        paper.setScopePayloadJson(scopePayloadJson(request));
        paper.setTotalScore(request.totalScore());
        paper.setStatus(PaperStatusEnum.DRAFT);
        paper.setCreatedAt(now);
        paper.setUpdatedAt(now);
        paperRepository.insert(paper);

        int sectionOrder = 1;
        for (PaperGenerateRequest.SectionRequest sectionRequest : request.sections()) {
            PaperSection section = createSection(paper.getId(), sectionRequest, sectionOrder++);
            List<Question> bankQuestions = request.strategy() == GenerationStrategyEnum.AI_ONLY
                    ? List.of()
                    : selectBankQuestions(ownerUserId, request, sectionRequest.questionType(), request.difficulty(), sectionRequest.questionCount());

            if (bankQuestions.size() < sectionRequest.questionCount() && request.strategy() == GenerationStrategyEnum.BANK_ONLY) {
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
            if (missing > 0 && request.strategy() != GenerationStrategyEnum.BANK_ONLY) {
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

    @Transactional
    public PaperResponse copy(Long ownerUserId, Long paperId) {
        Paper original = requirePaper(ownerUserId, paperId);
        LocalDateTime now = LocalDateTime.now();
        Paper copy = new Paper();
        copy.setOwnerUserId(ownerUserId);
        copy.setTitle(original.getTitle() + " (副本)");
        copy.setGrade(original.getGrade());
        copy.setPublisher(original.getPublisher());
        copy.setSubject(original.getSubject());
        copy.setVolume(original.getVolume());
        copy.setUnit(original.getUnit());
        copy.setChapter(original.getChapter());
        copy.setScopeType(original.getScopeType());
        copy.setScopePayloadJson(original.getScopePayloadJson());
        copy.setTotalScore(original.getTotalScore());
        copy.setStatus(PaperStatusEnum.DRAFT);
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        paperRepository.insert(copy);

        List<PaperSection> originalSections = paperSectionRepository.selectList(
                new LambdaQueryWrapper<PaperSection>()
                        .eq(PaperSection::getPaperId, paperId)
                        .orderByAsc(PaperSection::getSortOrder));

        for (PaperSection originalSection : originalSections) {
            PaperSection copySection = new PaperSection();
            copySection.setPaperId(copy.getId());
            copySection.setTitle(originalSection.getTitle());
            copySection.setQuestionType(originalSection.getQuestionType());
            copySection.setQuestionCount(originalSection.getQuestionCount());
            copySection.setScorePerQuestion(originalSection.getScorePerQuestion());
            copySection.setSubtotalScore(originalSection.getSubtotalScore());
            copySection.setSortOrder(originalSection.getSortOrder());
            paperSectionRepository.insert(copySection);

            List<PaperQuestion> originalQuestions = paperQuestionRepository.selectList(
                    new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getSectionId, originalSection.getId())
                            .orderByAsc(PaperQuestion::getSortOrder));

            for (PaperQuestion originalQuestion : originalQuestions) {
                PaperQuestion copyQuestion = new PaperQuestion();
                copyQuestion.setPaperId(copy.getId());
                copyQuestion.setSectionId(copySection.getId());
                copyQuestion.setSourceQuestionId(originalQuestion.getSourceQuestionId());
                copyQuestion.setSource(originalQuestion.getSource());
                copyQuestion.setStemSnapshot(originalQuestion.getStemSnapshot());
                copyQuestion.setContentSnapshotJson(originalQuestion.getContentSnapshotJson());
                copyQuestion.setAnswerSnapshotJson(originalQuestion.getAnswerSnapshotJson());
                copyQuestion.setAnalysisSnapshot(originalQuestion.getAnalysisSnapshot());
                copyQuestion.setScore(originalQuestion.getScore());
                copyQuestion.setSortOrder(originalQuestion.getSortOrder());
                paperQuestionRepository.insert(copyQuestion);
            }
        }

        return loadPaper(ownerUserId, copy.getId());
    }

    @Transactional
    public PaperResponse regenerate(Long ownerUserId, Long paperId) {
        Paper original = requirePaper(ownerUserId, paperId);
        List<PaperSection> originalSections = paperSectionRepository.selectList(
                new LambdaQueryWrapper<PaperSection>()
                        .eq(PaperSection::getPaperId, paperId)
                        .orderByAsc(PaperSection::getSortOrder));

        List<PaperGenerateRequest.SectionRequest> sectionRequests = originalSections.stream()
                .map(section -> new PaperGenerateRequest.SectionRequest(
                        section.getTitle(),
                        section.getQuestionType(),
                        section.getQuestionCount(),
                        section.getScorePerQuestion()
                ))
                .toList();

        paperQuestionRepository.delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId));
        paperSectionRepository.delete(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId));

        ScopeSnapshot scope = scopeSnapshot(original);
        PaperGenerateRequest request = new PaperGenerateRequest(
                original.getTitle() + " (重新组卷)",
                original.getGrade(),
                original.getPublisher(),
                original.getSubject(),
                original.getVolume(),
                scope.scopeType(),
                scope.units(),
                scope.chapters(),
                original.getTotalScore(),
                GenerationStrategyEnum.BANK_FIRST,
                null,
                sectionRequests
        );

        return generate(ownerUserId, request);
    }

    @Transactional
    public void save(Long ownerUserId, Long paperId) {
        Paper paper = requirePaper(ownerUserId, paperId);
        if (paper.getStatus() == PaperStatusEnum.SAVED) {
            return;
        }
        paper.setStatus(PaperStatusEnum.SAVED);
        paper.setUpdatedAt(LocalDateTime.now());
        paperRepository.updateById(paper);
    }

    @Transactional
    public void delete(Long ownerUserId, Long paperId) {
        Paper paper = requirePaper(ownerUserId, paperId);
        paperQuestionRepository.delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId));
        paperSectionRepository.delete(new LambdaQueryWrapper<PaperSection>()
                .eq(PaperSection::getPaperId, paperId));
        paperRepository.deleteById(paper);
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
        snapshot.setSource(QuestionSourceEnum.AI);
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
        DifficultyEnum difficulty = request.difficulty() == null ? DifficultyEnum.MEDIUM : request.difficulty();
        return aiQuestionClient.generate(new AiQuestionGenerationRequest(
                request.grade(), request.publisher(), request.subject(), request.volume(), scopeDescription(request),
                section.questionType(), difficulty, missing, section.scorePerQuestion()
        ));
    }

    private int countAvailable(Long ownerUserId, PaperGenerateRequest request, QuestionTypeEnum type, DifficultyEnum difficulty) {
        return Math.toIntExact(questionRepository.selectCount(scopeQuery(ownerUserId, request, type, difficulty)));
    }

    private List<Question> selectBankQuestions(
            Long ownerUserId,
            PaperGenerateRequest request,
            QuestionTypeEnum type,
            DifficultyEnum difficulty,
            int limit
    ) {
        return questionRepository.selectList(scopeQuery(ownerUserId, request, type, difficulty))
                .stream()
                .sorted(Comparator.comparing(Question::getUsageCount, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(Question::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    private LambdaQueryWrapper<Question> scopeQuery(Long ownerUserId, PaperGenerateRequest request, QuestionTypeEnum type, DifficultyEnum difficulty) {
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<Question>()
                .eq(Question::getOwnerUserId, ownerUserId)
                .eq(Question::getGrade, request.grade())
                .eq(Question::getPublisher, request.publisher())
                .eq(Question::getSubject, request.subject())
                .eq(Question::getVolume, request.volume())
                .eq(Question::getQuestionType, type)
                .eq(difficulty != null, Question::getDifficulty, difficulty);

        switch (request.scopeType()) {
            case CHAPTERS -> query.and(group -> {
                List<Map.Entry<String, List<String>>> entries = new ArrayList<>(chapterGroups(request).entrySet());
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<String, List<String>> entry = entries.get(i);
                    if (i == 0) {
                        group.eq(Question::getUnit, entry.getKey()).in(Question::getChapter, entry.getValue());
                    } else {
                        group.or(branch -> branch.eq(Question::getUnit, entry.getKey()).in(Question::getChapter, entry.getValue()));
                    }
                }
            });
            case UNITS -> query.in(Question::getUnit, request.units());
            case VOLUME -> {
            }
        }
        return query;
    }

    private Map<String, List<String>> chapterGroups(PaperGenerateRequest request) {
        return request.chapters().stream().collect(Collectors.groupingBy(
                ChapterScope::unit,
                LinkedHashMap::new,
                Collectors.mapping(ChapterScope::chapter, Collectors.toList())
        ));
    }

    private void validateScope(PaperGenerateRequest request) {
        if (request.scopeType() == null) {
            throw new BusinessException("组卷范围类型不能为空");
        }
        switch (request.scopeType()) {
            case CHAPTERS -> {
                if (request.chapters() == null || request.chapters().isEmpty()) {
                    throw new BusinessException("CHAPTERS 范围必须至少选择一个章节");
                }
                if (request.chapters().stream().anyMatch(scope -> isBlank(scope.unit()) || isBlank(scope.chapter()))) {
                    throw new BusinessException("CHAPTERS 范围中的单元和章节不能为空");
                }
            }
            case UNITS -> {
                if (request.units() == null || request.units().isEmpty()) {
                    throw new BusinessException("UNITS 范围必须至少选择一个单元");
                }
                if (request.units().stream().anyMatch(this::isBlank)) {
                    throw new BusinessException("UNITS 范围中的单元不能为空");
                }
            }
            case VOLUME -> {
            }
        }
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

    private ScopeDisplay scopeDisplay(PaperGenerateRequest request) {
        return switch (request.scopeType()) {
            case CHAPTERS -> new ScopeDisplay(
                    request.chapters().stream().map(ChapterScope::unit).distinct().collect(Collectors.joining(", ")),
                    request.chapters().stream().map(scope -> scope.unit() + " / " + scope.chapter()).collect(Collectors.joining(", "))
            );
            case UNITS -> new ScopeDisplay(String.join(", ", request.units()), ALL_CHAPTERS_DISPLAY);
            case VOLUME -> new ScopeDisplay(WHOLE_VOLUME_DISPLAY, ALL_CHAPTERS_DISPLAY);
        };
    }

    private String scopeDescription(PaperGenerateRequest request) {
        return switch (request.scopeType()) {
            case CHAPTERS -> "精确章节：" + request.chapters().stream()
                    .map(scope -> scope.unit() + " / " + scope.chapter())
                    .collect(Collectors.joining("; "));
            case UNITS -> "单元：" + String.join(", ", request.units()) + "（全部章节）";
            case VOLUME -> "整册";
        };
    }

    private String scopePayloadJson(PaperGenerateRequest request) {
        try {
            return objectMapper.writeValueAsString(new ScopeSnapshot(request.scopeType(), request.units(), request.chapters()));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("组卷范围序列化失败：" + ex.getMessage());
        }
    }

    private ScopeSnapshot scopeSnapshot(Paper paper) {
        if (paper.getScopePayloadJson() != null && !paper.getScopePayloadJson().isBlank()) {
            try {
                return objectMapper.readValue(paper.getScopePayloadJson(), ScopeSnapshot.class);
            } catch (JsonProcessingException ex) {
                throw new BusinessException("组卷范围解析失败：" + ex.getMessage());
            }
        }
        return new ScopeSnapshot(PaperScopeTypeEnum.CHAPTERS, null, inferLegacyChapterScopes(paper.getUnit(), paper.getChapter()));
    }

    private List<ChapterScope> inferLegacyChapterScopes(String unit, String chapters) {
        return Arrays.stream(chapters.split(","))
                .map(String::trim)
                .filter(chapter -> !chapter.isBlank())
                .map(chapter -> new ChapterScope(unit, chapter))
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ScopeDisplay(String unit, String chapter) {
    }

    private record ScopeSnapshot(
            PaperScopeTypeEnum scopeType,
            List<String> units,
            List<ChapterScope> chapters
    ) {
    }
}
