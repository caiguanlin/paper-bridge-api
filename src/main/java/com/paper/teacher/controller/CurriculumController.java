package com.paper.teacher.controller;

import com.paper.teacher.common.ApiResponse;
import com.paper.teacher.modules.curriculum.service.CurriculumService;
import com.paper.teacher.modules.curriculum.dto.CurriculumResponse;
import com.paper.teacher.modules.curriculum.dto.CurriculumSearchRequest;
import com.paper.teacher.modules.curriculum.dto.CurriculumUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
public class CurriculumController {
    private final CurriculumService curriculumService;

    @GetMapping
    public ApiResponse<List<CurriculumResponse>> search(@ModelAttribute CurriculumSearchRequest request) {
        return ApiResponse.ok(curriculumService.search(request));
    }

    @PostMapping
    public ApiResponse<CurriculumResponse> create(@Valid @RequestBody CurriculumUpsertRequest request) {
        return ApiResponse.ok(curriculumService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CurriculumResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CurriculumUpsertRequest request
    ) {
        return ApiResponse.ok(curriculumService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        curriculumService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/tree")
    public ApiResponse<List<CurriculumService.CurriculumTreeNode>> tree() {
        return ApiResponse.ok(curriculumService.tree());
    }
}
