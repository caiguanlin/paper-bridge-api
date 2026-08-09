package com.paper.teacher.modules.paper.support;

import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.modules.ai.dto.AiQuestionGenerationResponse;
import com.paper.teacher.modules.paper.entity.PaperQuestion;
import com.paper.teacher.modules.question.entity.Question;

import java.math.BigDecimal;

public final class PaperSnapshots {
    private PaperSnapshots() {
    }

    public static PaperQuestion fromBankQuestion(
            Long paperId,
            Long sectionId,
            Question question,
            BigDecimal score,
            int sortOrder
    ) {
        return snapshot(
                paperId,
                sectionId,
                question.getId(),
                question.getSource(),
                question.getStem(),
                question.getContentJson(),
                question.getAnswerJson(),
                question.getAnalysis(),
                score,
                sortOrder
        );
    }

    public static PaperQuestion fromAiQuestion(
            Long paperId,
            Long sectionId,
            AiQuestionGenerationResponse response,
            BigDecimal score,
            int sortOrder
    ) {
        return snapshot(
                paperId,
                sectionId,
                null,
                QuestionSourceEnum.AI,
                response.stem(),
                response.contentJson(),
                response.answerJson(),
                response.analysis(),
                score,
                sortOrder
        );
    }

    public static PaperQuestion copyOf(PaperQuestion original, Long paperId, Long sectionId) {
        return snapshot(
                paperId,
                sectionId,
                original.getSourceQuestionId(),
                original.getSource(),
                original.getStemSnapshot(),
                original.getContentSnapshotJson(),
                original.getAnswerSnapshotJson(),
                original.getAnalysisSnapshot(),
                original.getScore(),
                original.getSortOrder()
        );
    }

    private static PaperQuestion snapshot(
            Long paperId,
            Long sectionId,
            Long sourceQuestionId,
            QuestionSourceEnum source,
            String stem,
            String contentJson,
            String answerJson,
            String analysis,
            BigDecimal score,
            Integer sortOrder
    ) {
        PaperQuestion snapshot = new PaperQuestion();
        snapshot.setPaperId(paperId);
        snapshot.setSectionId(sectionId);
        snapshot.setSourceQuestionId(sourceQuestionId);
        snapshot.setSource(source);
        snapshot.setStemSnapshot(stem);
        snapshot.setContentSnapshotJson(contentJson);
        snapshot.setAnswerSnapshotJson(answerJson);
        snapshot.setAnalysisSnapshot(analysis);
        snapshot.setScore(score);
        snapshot.setSortOrder(sortOrder);
        return snapshot;
    }
}
