package com.paper.teacher.question;

import com.paper.teacher.common.ApiResponse;
import com.paper.teacher.common.CurrentTeacherProvider;
import com.paper.teacher.question.dto.QuestionCreateRequest;
import com.paper.teacher.question.dto.QuestionImportResult;
import com.paper.teacher.question.dto.QuestionResponse;
import com.paper.teacher.question.dto.QuestionSearchRequest;
import com.paper.teacher.question.importexcel.QuestionExcelImporter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;
    private final QuestionExcelImporter questionExcelImporter;
    private final CurrentTeacherProvider currentTeacherProvider;

    @GetMapping
    public ApiResponse<List<QuestionResponse>> search(@ModelAttribute QuestionSearchRequest request) {
        return ApiResponse.ok(questionService.search(currentTeacherProvider.id(), request));
    }

    @PostMapping
    public ApiResponse<QuestionResponse> create(
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        return ApiResponse.ok(QuestionResponse.from(questionService.create(currentTeacherProvider.id(), request, QuestionSource.MANUAL)));
    }

    @PostMapping("/import/excel")
    public ApiResponse<QuestionImportResult> importExcel(
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(questionExcelImporter.importFile(currentTeacherProvider.id(), file));
    }
}
