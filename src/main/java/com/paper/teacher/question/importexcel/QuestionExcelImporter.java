package com.paper.teacher.question.importexcel;

import com.paper.teacher.question.Difficulty;
import com.paper.teacher.question.QuestionSource;
import com.paper.teacher.question.QuestionType;
import com.paper.teacher.question.dto.QuestionCreateRequest;
import com.paper.teacher.question.dto.QuestionImportResult;
import com.paper.teacher.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionExcelImporter {
    private final QuestionService questionService;
    private final DataFormatter dataFormatter = new DataFormatter();

    public QuestionImportResult importFile(Long ownerUserId, MultipartFile file) {
        int successCount = 0;
        List<QuestionImportResult.RowError> errors = new ArrayList<>();
        try (var workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                try {
                    QuestionCreateRequest request = toRequest(row);
                    questionService.create(ownerUserId, request, QuestionSource.EXCEL_IMPORT);
                    successCount++;
                } catch (RuntimeException exception) {
                    errors.add(new QuestionImportResult.RowError(i + 1, "行数据", exception.getMessage()));
                }
            }
        } catch (Exception exception) {
            errors.add(new QuestionImportResult.RowError(0, "文件", "Excel 文件无法读取"));
        }
        return new QuestionImportResult(successCount, errors.size(), errors);
    }

    private QuestionCreateRequest toRequest(Row row) {
        return new QuestionCreateRequest(
                cell(row, 0),
                cell(row, 1),
                cell(row, 2),
                cell(row, 3),
                cell(row, 4),
                cell(row, 5),
                QuestionType.valueOf(cell(row, 6)),
                Difficulty.valueOf(cell(row, 7)),
                cell(row, 8),
                cell(row, 9),
                cell(row, 10),
                cell(row, 11)
        );
    }

    private String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        return dataFormatter.formatCellValue(cell).trim();
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < 12; i++) {
            if (!cell(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
