package com.paper.teacher.question;

import com.paper.teacher.constant.enums.QuestionSourceEnum;

import com.paper.teacher.modules.question.QuestionService;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.importexcel.QuestionExcelImporter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class QuestionExcelImporterTest {
    @Test
    void importsValidRowsAndReportsInvalidRows() throws Exception {
        QuestionService questionService = mock(QuestionService.class);
        doThrow(new IllegalArgumentException("题目内容 JSON 不合法"))
                .when(questionService)
                .create(eq(1L), any(QuestionCreateRequest.class), eq(QuestionSourceEnum.EXCEL_IMPORT));

        QuestionExcelImporter importer = new QuestionExcelImporter(questionService);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes()
        );

        var result = importer.importFile(1L, file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors()).first().extracting("rowNumber").isEqualTo(2);
    }

    private byte[] workbookBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("questions");
            sheet.createRow(0);
            var row = sheet.createRow(1);
            String[] values = {
                    "三年级", "人教版", "MATH", "上册", "第三单元", "测量",
                    "SINGLE_CHOICE", "EASY", "示例题", "{\"options\":[\"A\",\"B\"]}",
                    "{\"correctOption\":\"A\"}", "解析"
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
