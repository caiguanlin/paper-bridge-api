package com.paper.teacher.modules.question.importexcel;

import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.dto.QuestionImportResult;
import com.paper.teacher.modules.question.service.QuestionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionExcelImporterTest {
    private static final String[] VALID_ROW = {
            "三年级", "人教版", "MATH", "上册", "第一单元", "第一节",
            "TRUE_FALSE", "MEDIUM", "1+1=2", "{\"statement\":\"1+1=2\"}", "{\"correctBoolean\":true}", ""
    };

    private final QuestionService questionService = Mockito.mock(QuestionService.class);
    private final QuestionExcelImporter importer = new QuestionExcelImporter(questionService);

    @Test
    void unreadableFileFailsLoudlyInsteadOfReturningARowError() {
        MultipartFile file = new MockMultipartFile("file", "questions.xlsx", null, "not-an-excel-file".getBytes());

        BusinessException exception = assertThrows(BusinessException.class, () -> importer.importFile(1L, file));

        assertTrue(exception.getMessage().startsWith("Excel 文件无法"), exception.getMessage());
        assertTrue(exception.getCause() != null);
    }

    @Test
    void emptyUploadIsRejected() {
        MultipartFile file = new MockMultipartFile("file", "questions.xlsx", null, new byte[0]);

        assertEquals("上传的 Excel 文件为空", assertThrows(BusinessException.class,
                () -> importer.importFile(1L, file)).getMessage());
    }

    @Test
    void invalidEnumCellReportsAllowedValues() throws IOException {
        String[] badType = VALID_ROW.clone();
        badType[6] = "SINGLE_CHOICE_X";

        QuestionImportResult result = importer.importFile(1L, workbook(badType));

        assertEquals(0, result.successCount());
        assertEquals(1, result.failureCount());
        QuestionImportResult.RowError error = result.errors().getFirst();
        assertEquals(2, error.rowNumber());
        assertTrue(error.message().contains("SINGLE_CHOICE_X"), error.message());
        assertTrue(error.message().contains("TRUE_FALSE"), error.message());
    }

    @Test
    void rowFailureWithoutMessageStillReportsSomething() throws IOException {
        Mockito.when(questionService.create(Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenThrow(new NullPointerException());

        QuestionImportResult result = importer.importFile(1L, workbook(VALID_ROW));

        assertEquals(1, result.failureCount());
        assertEquals("NullPointerException", result.errors().getFirst().message());
    }

    @Test
    void validRowIsImported() throws IOException {
        QuestionImportResult result = importer.importFile(1L, workbook(VALID_ROW));

        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        Mockito.verify(questionService).create(
                Mockito.eq(1L),
                Mockito.any(QuestionCreateRequest.class),
                Mockito.eq(QuestionSourceEnum.EXCEL_IMPORT));
    }

    private MultipartFile workbook(String[] dataRow) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < dataRow.length; i++) {
                header.createCell(i).setCellValue("column" + i);
            }
            Row row = sheet.createRow(1);
            for (int i = 0; i < dataRow.length; i++) {
                row.createCell(i).setCellValue(dataRow[i]);
            }
            workbook.write(output);
            return new MockMultipartFile("file", "questions.xlsx", null, output.toByteArray());
        }
    }
}
