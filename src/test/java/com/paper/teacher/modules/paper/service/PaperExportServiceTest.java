package com.paper.teacher.modules.paper.service;

import com.paper.teacher.constant.enums.PaperStatusEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.paper.dto.PaperResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperExportServiceTest {
    private static final long OWNER_ID = 4L;

    @Mock
    private PaperGenerationService paperGenerationService;
    @InjectMocks
    private PaperExportService paperExportService;

    @Test
    void studentHtmlEscapesMarkupAndHidesAnswers() {
        when(paperGenerationService.loadPaper(OWNER_ID, 70L)).thenReturn(paper());

        String html = paperExportService.printableHtml(OWNER_ID, 70L, "student");

        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains("<title>期中&lt;卷&gt;</title>"));
        assertTrue(html.contains("1. 1+1&lt;3 吗（2 分）"));
        assertFalse(html.contains("答案："));
        assertFalse(html.contains("<script>"));
    }

    @Test
    void teacherHtmlIncludesAnswerAndAnalysis() {
        when(paperGenerationService.loadPaper(OWNER_ID, 70L)).thenReturn(paper());

        String html = paperExportService.printableHtml(OWNER_ID, 70L, "TEACHER");

        assertTrue(html.contains("<strong>答案：</strong>"));
        assertTrue(html.contains("<strong>解析：</strong>基础运算"));
    }

    @Test
    void wordExportContainsTitleSectionAndQuestionParagraphs() throws Exception {
        when(paperGenerationService.loadPaper(OWNER_ID, 70L)).thenReturn(paper());

        byte[] bytes = paperExportService.exportWord(OWNER_ID, 70L, "student");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<String> paragraphs = document.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertTrue(paragraphs.contains("期中<卷>"));
            assertTrue(paragraphs.contains("总分：2"));
            assertTrue(paragraphs.contains("一、判断题（共 2 分）"));
            assertTrue(paragraphs.contains("1. 1+1<3 吗（2 分）"));
            assertFalse(paragraphs.stream().anyMatch(text -> text.startsWith("答案：")));
        }
    }

    @Test
    void wordTeacherExportIncludesAnswersAndEmptyAnalysisWhenMissing() throws Exception {
        when(paperGenerationService.loadPaper(OWNER_ID, 70L)).thenReturn(paperWithoutAnalysis());

        byte[] bytes = paperExportService.exportWord(OWNER_ID, 70L, "teacher");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<String> paragraphs = document.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertTrue(paragraphs.contains("答案：{\"correctBoolean\":true}"));
            assertTrue(paragraphs.contains("解析："));
        }
    }

    private static PaperResponse paper() {
        return paper("基础运算");
    }

    private static PaperResponse paperWithoutAnalysis() {
        return paper(null);
    }

    private static PaperResponse paper(String analysis) {
        PaperResponse.QuestionResponse question = new PaperResponse.QuestionResponse(
                7000L, null, QuestionSourceEnum.AI, "1+1<3 吗",
                "{\"statement\":\"1+1<3\"}", "{\"correctBoolean\":true}", analysis, new BigDecimal("2"), 1);
        PaperResponse.SectionResponse section = new PaperResponse.SectionResponse(
                700L, "一、判断题", QuestionTypeEnum.TRUE_FALSE, 1, new BigDecimal("2"),
                new BigDecimal("2"), 1, List.of(question));
        return new PaperResponse(70L, "期中<卷>", "三年级", "人教版", "CHINESE", "上册", "整册", "全部章节",
                new BigDecimal("2"), PaperStatusEnum.DRAFT, LocalDateTime.now(), List.of(section));
    }
}
