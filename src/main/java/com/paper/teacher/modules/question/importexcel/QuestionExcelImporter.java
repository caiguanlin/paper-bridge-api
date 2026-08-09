package com.paper.teacher.modules.question.importexcel;

import com.paper.teacher.constant.enums.DifficultyEnum;

import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.modules.question.dto.QuestionCreateRequest;
import com.paper.teacher.modules.question.dto.QuestionImportResult;
import com.paper.teacher.modules.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionExcelImporter {
    private final QuestionService questionService;
    private final DataFormatter dataFormatter = new DataFormatter();

    public QuestionImportResult importFile(Long ownerUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传的 Excel 文件为空");
        }
        int successCount = 0;
        List<QuestionImportResult.RowError> errors = new ArrayList<>();
        try (var workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException("Excel 文件不包含任何工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                try {
                    QuestionCreateRequest request = toRequest(row);
                    questionService.create(ownerUserId, request, QuestionSourceEnum.EXCEL_IMPORT);
                    successCount++;
                } catch (BusinessException exception) {
                    log.warn("Excel import rejected row {}: {}", i + 1, exception.getMessage());
                    errors.add(new QuestionImportResult.RowError(i + 1, "行数据", exception.getMessage()));
                } catch (RuntimeException exception) {
                    log.error("Excel import failed on row {}", i + 1, exception);
                    errors.add(new QuestionImportResult.RowError(i + 1, "行数据", describe(exception)));
                }
            }
        } catch (IOException | EncryptedDocumentException exception) {
            log.warn("Excel import could not read uploaded file {}", file.getOriginalFilename(), exception);
            throw new BusinessException("Excel 文件无法读取：" + describe(exception), exception);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Excel import failed while parsing file {}", file.getOriginalFilename(), exception);
            throw new BusinessException("Excel 文件无法解析：" + describe(exception), exception);
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
                parseEnum(QuestionTypeEnum.class, cell(row, 6), "题型"),
                parseEnum(DifficultyEnum.class, cell(row, 7), "难度"),
                cell(row, 8),
                cell(row, 9),
                cell(row, 10),
                cell(row, 11)
        );
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(label + " \"" + value + "\" 不合法，可选值："
                    + Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", ")),
                    exception);
        }
    }

    private String describe(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
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
