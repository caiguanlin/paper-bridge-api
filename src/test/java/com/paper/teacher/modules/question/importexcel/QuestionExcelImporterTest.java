package com.paper.teacher.modules.question.importexcel;

import com.paper.teacher.common.BusinessException;
import com.paper.teacher.constant.enums.DifficultyEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.dto.QuestionImportResult;
import com.paper.teacher.modules.question.service.QuestionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionExcelImporterTest {
    private static final List<String> VALID_ROW = List.of(
            "三年级", "人教版", "CHINESE", "上册", "第一单元", "第一课",
            "DICTATION", "EASY", "默写静夜思",
            "{\"prompt\":\"默写静夜思\"}", "{\"expectedText\":\"床前明月光\"}", "考查背诵");

    @Mock
    private QuestionService questionService;
    @InjectMocks
    private QuestionExcelImporter importer;

    @Test
    void importsValidRowsAndSkipsBlankRows() throws Exception {
        MultipartFile file = workbook(VALID_ROW, List.of("", "", "", "", "", "", "", "", "", "", "", ""), VALID_ROW);

        QuestionImportResult result = importer.importFile(5L, file);

        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        ArgumentCaptor<QuestionCreateRequest> captor = ArgumentCaptor.forClass(QuestionCreateRequest.class);
        verify(questionService, org.mockito.Mockito.times(2))
                .create(eq(5L), captor.capture(), eq(QuestionSourceEnum.EXCEL_IMPORT));
        QuestionCreateRequest request = captor.getValue();
        assertEquals("三年级", request.grade());
        assertEquals(QuestionTypeEnum.DICTATION, request.questionType());
        assertEquals(DifficultyEnum.EASY, request.difficulty());
        assertEquals("考查背诵", request.analysis());
    }

    @Test
    void reportsRowNumberWhenQuestionTypeIsUnknown() throws Exception {
        List<String> badRow = new java.util.ArrayList<>(VALID_ROW);
        badRow.set(6, "UNKNOWN_TYPE");
        MultipartFile file = workbook(badRow);

        QuestionImportResult result = importer.importFile(5L, file);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(2, result.errors().getFirst().rowNumber());
        assertEquals("行数据", result.errors().getFirst().fieldName());
        verify(questionService, never()).create(any(), any(), any());
    }

    @Test
    void reportsRowErrorWhenServiceRejectsRow() throws Exception {
        doThrow(new BusinessException("默写题必须包含 prompt"))
                .when(questionService).create(any(), any(), any());
        MultipartFile file = workbook(VALID_ROW);

        QuestionImportResult result = importer.importFile(5L, file);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals("默写题必须包含 prompt", result.errors().getFirst().message());
    }

    @Test
    void reportsFileErrorWhenWorkbookCannotBeRead() {
        MultipartFile file = new MockMultipartFile("file", "questions.xlsx",
                "application/vnd.ms-excel", "not-an-excel-file".getBytes());

        QuestionImportResult result = importer.importFile(5L, file);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(0, result.errors().getFirst().rowNumber());
        assertEquals("文件", result.errors().getFirst().fieldName());
        assertTrue(result.errors().getFirst().message().contains("无法读取"));
    }

    @SafeVarargs
    private MultipartFile workbook(List<String>... dataRows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < 12; i++) {
                header.createCell(i).setCellValue("column" + i);
            }
            int rowIndex = 1;
            for (List<String> values : dataRows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < values.size(); i++) {
                    row.createCell(i).setCellValue(values.get(i));
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "questions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
