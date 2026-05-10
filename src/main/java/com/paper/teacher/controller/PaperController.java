package com.paper.teacher.controller;

import com.paper.teacher.common.ApiResponse;
import com.paper.teacher.common.CurrentTeacherProvider;
import com.paper.teacher.modules.paper.PaperEditService;
import com.paper.teacher.modules.paper.PaperExportService;
import com.paper.teacher.modules.paper.PaperGenerationService;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest;
import com.paper.teacher.modules.paper.dto.PaperPlanPreview;
import com.paper.teacher.modules.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.modules.paper.dto.PaperResponse;
import com.paper.teacher.modules.paper.dto.PaperSummaryResponse;
import com.paper.teacher.modules.question.dto.QuestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperGenerationService paperGenerationService;
    private final PaperEditService paperEditService;
    private final PaperExportService paperExportService;
    private final CurrentTeacherProvider currentTeacherProvider;

    @PostMapping("/preview-plan")
    public ApiResponse<PaperPlanPreview> previewPlan(
            @Valid @RequestBody PaperGenerateRequest request
    ) {
        return ApiResponse.ok(paperGenerationService.preview(currentTeacherProvider.id(), request));
    }

    @PostMapping("/generate")
    public ApiResponse<PaperResponse> generate(
            @Valid @RequestBody PaperGenerateRequest request
    ) {
        return ApiResponse.ok(paperGenerationService.generate(currentTeacherProvider.id(), request));
    }

    @GetMapping
    public ApiResponse<List<PaperSummaryResponse>> list() {
        return ApiResponse.ok(paperGenerationService.list(currentTeacherProvider.id()));
    }

    @GetMapping("/{paperId}")
    public ApiResponse<PaperResponse> detail(@PathVariable Long paperId) {
        return ApiResponse.ok(paperGenerationService.loadPaper(currentTeacherProvider.id(), paperId));
    }

    @PatchMapping("/{paperId}/questions/{paperQuestionId}")
    public ApiResponse<PaperResponse> updateQuestion(
            @PathVariable Long paperId,
            @PathVariable Long paperQuestionId,
            @Valid @RequestBody PaperQuestionUpdateRequest request
    ) {
        Long teacherId = currentTeacherProvider.id();
        paperEditService.updateQuestion(teacherId, paperId, paperQuestionId, request);
        return ApiResponse.ok(paperGenerationService.loadPaper(teacherId, paperId));
    }

    @PostMapping("/{paperId}/questions/{paperQuestionId}/save-to-bank")
    public ApiResponse<QuestionResponse> saveToBank(
            @PathVariable Long paperId,
            @PathVariable Long paperQuestionId
    ) {
        return ApiResponse.ok(QuestionResponse.from(paperEditService.saveToBank(currentTeacherProvider.id(), paperId, paperQuestionId)));
    }

    @GetMapping(value = "/{paperId}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String print(
            @PathVariable Long paperId,
            @RequestParam(defaultValue = "student") String version
    ) {
        return paperExportService.printableHtml(currentTeacherProvider.id(), paperId, version);
    }

    @PostMapping("/{paperId}/export/word")
    public ResponseEntity<byte[]> exportWord(
            @PathVariable Long paperId,
            @RequestParam(defaultValue = "student") String version
    ) {
        byte[] body = paperExportService.exportWord(currentTeacherProvider.id(), paperId, version);
        String filename = "paper-" + paperId + "-" + version + ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(body);
    }

    @PostMapping("/{paperId}/copy")
    public ApiResponse<PaperResponse> copy(@PathVariable Long paperId) {
        return ApiResponse.ok(paperGenerationService.copy(currentTeacherProvider.id(), paperId));
    }

    @PostMapping("/{paperId}/regenerate")
    public ApiResponse<PaperResponse> regenerate(@PathVariable Long paperId) {
        return ApiResponse.ok(paperGenerationService.regenerate(currentTeacherProvider.id(), paperId));
    }

    @PostMapping("/{paperId}/save")
    public ApiResponse<Void> save(@PathVariable Long paperId) {
        paperGenerationService.save(currentTeacherProvider.id(), paperId);
        return ApiResponse.ok((Void) null);
    }

    @DeleteMapping("/{paperId}")
    public ApiResponse<Void> delete(@PathVariable Long paperId) {
        paperGenerationService.delete(currentTeacherProvider.id(), paperId);
        return ApiResponse.ok((Void) null);
    }
}
