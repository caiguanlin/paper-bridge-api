package com.paper.teacher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.common.CurrentTeacherProvider;
import com.paper.teacher.common.GlobalExceptionHandler;
import com.paper.teacher.constant.enums.GenerationStrategyEnum;
import com.paper.teacher.constant.enums.PaperScopeTypeEnum;
import com.paper.teacher.constant.enums.PaperStatusEnum;
import com.paper.teacher.constant.enums.QuestionSourceEnum;
import com.paper.teacher.constant.enums.QuestionTypeEnum;
import com.paper.teacher.modules.paper.dto.PaperGenerateRequest;
import com.paper.teacher.modules.paper.dto.PaperPlanPreview;
import com.paper.teacher.modules.paper.dto.PaperQuestionUpdateRequest;
import com.paper.teacher.modules.paper.dto.PaperResponse;
import com.paper.teacher.modules.paper.dto.PaperSummaryResponse;
import com.paper.teacher.modules.paper.service.PaperEditService;
import com.paper.teacher.modules.paper.service.PaperExportService;
import com.paper.teacher.modules.paper.service.PaperGenerationService;
import com.paper.teacher.modules.question.entity.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaperControllerTest {
    private static final long TEACHER_ID = 4L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private PaperGenerationService paperGenerationService;
    @Mock
    private PaperEditService paperEditService;
    @Mock
    private PaperExportService paperExportService;
    @Mock
    private CurrentTeacherProvider currentTeacherProvider;
    @InjectMocks
    private PaperController paperController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paperController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(currentTeacherProvider.id()).thenReturn(TEACHER_ID);
    }

    @Test
    void previewPlanReturnsPlanForCurrentTeacher() throws Exception {
        when(paperGenerationService.preview(eq(TEACHER_ID), any())).thenReturn(new PaperPlanPreview(
                new BigDecimal("10"), new BigDecimal("10"),
                List.of(new PaperPlanPreview.SectionPreview(
                        "一、选择题", QuestionTypeEnum.SINGLE_CHOICE, 5, 3, 2, new BigDecimal("10")))));

        mockMvc.perform(post("/api/papers/preview-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(generateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sections[0].aiSupplementCount").value(2));
    }

    @Test
    void invalidGenerateRequestReturnsValidationMessage() throws Exception {
        PaperGenerateRequest invalid = new PaperGenerateRequest(
                " ", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.VOLUME, null, null,
                new BigDecimal("10"), GenerationStrategyEnum.BANK_ONLY, null, List.of());

        mockMvc.perform(post("/api/papers/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void businessFailureIsTranslatedToBadRequest() throws Exception {
        when(paperGenerationService.generate(eq(TEACHER_ID), any()))
                .thenThrow(new BusinessException("一、选择题题库数量不足，缺少 4 道题"));

        mockMvc.perform(post("/api/papers/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(generateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("一、选择题题库数量不足，缺少 4 道题"));
    }

    @Test
    void listAndDetailReadThroughToGenerationService() throws Exception {
        when(paperGenerationService.list(TEACHER_ID)).thenReturn(List.of(new PaperSummaryResponse(
                70L, "期中卷", "三年级", "人教版", "CHINESE", "上册", "整册", "全部章节",
                new BigDecimal("10"), PaperStatusEnum.DRAFT, LocalDateTime.now())));
        when(paperGenerationService.loadPaper(TEACHER_ID, 70L)).thenReturn(paperResponse());

        mockMvc.perform(get("/api/papers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(70));
        mockMvc.perform(get("/api/papers/70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("期中卷"));
    }

    @Test
    void updateQuestionReturnsReloadedPaper() throws Exception {
        when(paperGenerationService.loadPaper(TEACHER_ID, 70L)).thenReturn(paperResponse());
        PaperQuestionUpdateRequest request = new PaperQuestionUpdateRequest(
                "新题干", "{}", "{}", "解析", new BigDecimal("2"));

        mockMvc.perform(patch("/api/papers/70/questions/7000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(70));

        verify(paperEditService).updateQuestion(TEACHER_ID, 70L, 7000L, request);
    }

    @Test
    void saveToBankReturnsCreatedBankQuestion() throws Exception {
        Question question = new Question();
        question.setId(31L);
        question.setStem("题干");
        question.setSource(QuestionSourceEnum.AI);
        when(paperEditService.saveToBank(TEACHER_ID, 70L, 7000L)).thenReturn(question);

        mockMvc.perform(post("/api/papers/70/questions/7000/save-to-bank"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.source").value("AI"));
    }

    @Test
    void printDefaultsToStudentVersion() throws Exception {
        when(paperExportService.printableHtml(TEACHER_ID, 70L, "student")).thenReturn("<html>student</html>");

        mockMvc.perform(get("/api/papers/70/print"))
                .andExpect(status().isOk())
                .andExpect(content().string("<html>student</html>"));
    }

    @Test
    void exportWordAttachesDocumentWithVersionedFilename() throws Exception {
        when(paperExportService.exportWord(TEACHER_ID, 70L, "teacher")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(post("/api/papers/70/export/word").param("version", "teacher"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("paper-70-teacher.docx")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void copyRegenerateSaveAndDeleteDelegateToService() throws Exception {
        when(paperGenerationService.copy(TEACHER_ID, 70L)).thenReturn(paperResponse());
        when(paperGenerationService.regenerate(TEACHER_ID, 70L)).thenReturn(paperResponse());

        mockMvc.perform(post("/api/papers/70/copy")).andExpect(status().isOk());
        mockMvc.perform(post("/api/papers/70/regenerate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/papers/70/save")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/papers/70")).andExpect(status().isOk());

        verify(paperGenerationService).save(TEACHER_ID, 70L);
        verify(paperGenerationService).delete(TEACHER_ID, 70L);
    }

    private static PaperGenerateRequest generateRequest() {
        return new PaperGenerateRequest(
                "期中卷", "三年级", "人教版", "CHINESE", "上册", PaperScopeTypeEnum.VOLUME, null, null,
                new BigDecimal("10"), GenerationStrategyEnum.BANK_FIRST, null,
                List.of(new PaperGenerateRequest.SectionRequest(
                        "一、选择题", QuestionTypeEnum.SINGLE_CHOICE, 5, new BigDecimal("2"))));
    }

    private static PaperResponse paperResponse() {
        return new PaperResponse(70L, "期中卷", "三年级", "人教版", "CHINESE", "上册", "整册", "全部章节",
                new BigDecimal("10"), PaperStatusEnum.DRAFT, LocalDateTime.now(), List.of());
    }
}
