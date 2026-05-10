package com.paper.teacher.modules.paper.service;

import com.paper.teacher.modules.paper.dto.PaperResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PaperExportService {
    private final PaperGenerationService paperGenerationService;

    public String printableHtml(Long ownerUserId, Long paperId, String version) {
        PaperResponse paper = paperGenerationService.loadPaper(ownerUserId, paperId);
        boolean teacherVersion = isTeacher(version);
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>")
                .append(escape(paper.title()))
                .append("</title><style>")
                .append("body{font-family:Arial,'Microsoft YaHei',sans-serif;margin:32px;color:#111}")
                .append(".paper{max-width:794px;margin:0 auto}.section{margin-top:24px}.question{margin:12px 0;break-inside:avoid}")
                .append(".answer{color:#166534;background:#f0fdf4;padding:8px;margin-top:6px}")
                .append("@media print{body{margin:0}.paper{box-shadow:none}}")
                .append("</style></head><body><main class=\"paper\">")
                .append("<h1>").append(escape(paper.title())).append("</h1>")
                .append("<p>总分：").append(paper.totalScore()).append("</p>");
        for (PaperResponse.SectionResponse section : paper.sections()) {
            html.append("<section class=\"section\"><h2>")
                    .append(escape(section.title()))
                    .append("（共 ").append(section.subtotalScore()).append(" 分）</h2>");
            int index = 1;
            for (PaperResponse.QuestionResponse question : section.questions()) {
                html.append("<div class=\"question\"><p>")
                        .append(index++).append(". ")
                        .append(escape(question.stemSnapshot()))
                        .append("（").append(question.score()).append(" 分）</p>");
                if (teacherVersion) {
                    html.append("<div class=\"answer\"><strong>答案：</strong>")
                            .append(escape(question.answerSnapshotJson()))
                            .append("<br><strong>解析：</strong>")
                            .append(escape(question.analysisSnapshot()))
                            .append("</div>");
                }
                html.append("</div>");
            }
            html.append("</section>");
        }
        html.append("</main></body></html>");
        return html.toString();
    }

    public byte[] exportWord(Long ownerUserId, Long paperId, String version) {
        PaperResponse paper = paperGenerationService.loadPaper(ownerUserId, paperId);
        boolean teacherVersion = isTeacher(version);
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            paragraph(document, paper.title(), true);
            paragraph(document, "总分：" + paper.totalScore(), false);
            for (PaperResponse.SectionResponse section : paper.sections()) {
                paragraph(document, section.title() + "（共 " + section.subtotalScore() + " 分）", true);
                int index = 1;
                for (PaperResponse.QuestionResponse question : section.questions()) {
                    paragraph(document, index++ + ". " + question.stemSnapshot() + "（" + question.score() + " 分）", false);
                    if (teacherVersion) {
                        paragraph(document, "答案：" + question.answerSnapshotJson(), false);
                        paragraph(document, "解析：" + nullToEmpty(question.analysisSnapshot()), false);
                    }
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Word 导出失败", exception);
        }
    }

    private void paragraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(nullToEmpty(text));
    }

    private boolean isTeacher(String version) {
        return "teacher".equalsIgnoreCase(version);
    }

    private String escape(String value) {
        return StringEscapeUtils.escapeHtml4(nullToEmpty(value));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
