package com.paper.teacher.common;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionBecomesBadRequestWithMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(new BusinessException("试卷不存在"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().data());
        assertEquals("试卷不存在", response.getBody().message());
    }

    @Test
    void bodyValidationErrorUsesFirstFieldMessage() throws Exception {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleValidation(methodArgumentNotValid("title", "试卷标题不能为空"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("试卷标题不能为空", response.getBody().message());
    }

    @Test
    void bodyValidationErrorFallsBackToGenericMessage() throws Exception {
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(methodArgumentNotValid(null, null));

        assertNotNull(response.getBody());
        assertEquals("请求参数不合法", response.getBody().message());
    }

    @Test
    void constraintViolationBecomesBadRequest() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleConstraint(new ConstraintViolationException("file 不能为空", Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("file 不能为空", response.getBody().message());
    }

    @Test
    void unknownExceptionBecomesServerErrorWithoutLeakingDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnknown(new IllegalStateException("jdbc connection refused"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("服务暂时不可用", response.getBody().message());
    }

    private static MethodArgumentNotValidException methodArgumentNotValid(String field, String message)
            throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        if (field != null) {
            bindingResult.addError(new org.springframework.validation.FieldError("request", field, message));
        }
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("handled", String.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void handled(String request) {
    }
}
