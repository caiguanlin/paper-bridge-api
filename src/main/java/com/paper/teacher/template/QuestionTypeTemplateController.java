package com.paper.teacher.template;

import com.paper.teacher.common.ApiResponse;
import com.paper.teacher.template.dto.QuestionTypeTemplateRequest;
import com.paper.teacher.template.dto.QuestionTypeTemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/question-type-templates")
@RequiredArgsConstructor
public class QuestionTypeTemplateController {
    private final QuestionTypeTemplateService templateService;

    @GetMapping
    public ApiResponse<List<QuestionTypeTemplateResponse>> list() {
        return ApiResponse.ok(templateService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionTypeTemplateResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(templateService.detail(id));
    }

    @PostMapping
    public ApiResponse<QuestionTypeTemplateResponse> create(
            @Valid @RequestBody QuestionTypeTemplateRequest request
    ) {
        return ApiResponse.ok(templateService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionTypeTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionTypeTemplateRequest request
    ) {
        return ApiResponse.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.ok(null);
    }
}
