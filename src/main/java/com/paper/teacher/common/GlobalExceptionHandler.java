package com.paper.teacher.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        log.warn("Business exception: {}", exception.getMessage(), exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(ExternalServiceException exception) {
        log.error("External service call failed: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException exception) {
        log.warn("Unauthenticated request: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("未认证或登录已过期，请重新登录"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("没有访问权限"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(this::describe)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "请求参数不合法";
        }
        log.warn("Request body validation failed: {}", message, exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException exception) {
        log.warn("Request constraint validation failed: {}", exception.getMessage(), exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.warn("Request body could not be parsed: {}", exception.getMessage(), exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail("请求体格式不合法，无法解析"));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingRequestValueException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        log.warn("Invalid request: {}", exception.getMessage(), exception);
        return ResponseEntity.badRequest().body(ApiResponse.fail("请求参数不合法：" + exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        log.warn("Upload rejected, file too large: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.fail("上传文件过大"));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedRequest(Exception exception) {
        HttpStatus status = exception instanceof HttpRequestMethodNotSupportedException
                ? HttpStatus.METHOD_NOT_ALLOWED
                : HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        log.warn("Unsupported request: {}", exception.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.fail(exception.getMessage()));
    }

    /**
     * 保留 Spring 已经定义好状态码的异常（如 404 NoResourceFoundException），避免被降级成 500。
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorResponse(ErrorResponseException exception) {
        log.warn("Request failed with status {}: {}", exception.getStatusCode(), exception.getMessage());
        return ResponseEntity.status(exception.getStatusCode())
                .body(ApiResponse.fail(exception.getBody().getDetail() == null
                        ? exception.getMessage()
                        : exception.getBody().getDetail()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception exception) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled server exception, errorId={}", errorId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("服务暂时不可用（错误编号 " + errorId + "）"));
    }

    private String describe(ObjectError error) {
        String message = error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage();
        return error instanceof FieldError fieldError ? fieldError.getField() + " " + message : message;
    }
}
