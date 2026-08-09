package com.paper.teacher.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.ErrorResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void externalServiceFailureIsReportedAsBadGateway() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleExternalService(new ExternalServiceException("DeepSeek 接口调用失败", new RuntimeException()));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DeepSeek 接口调用失败", response.getBody().message());
    }

    @Test
    void missingAuthenticationIsReportedAsUnauthorized() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuthentication(new AuthenticationCredentialsNotFoundException("no teacher"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void accessDeniedIsReportedAsForbidden() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void errorResponseExceptionKeepsItsOwnStatus() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleErrorResponse(new ErrorResponseException(HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void unknownFailureCarriesTraceableErrorId() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().matches("服务暂时不可用（错误编号 [0-9a-f]{8}）"));
    }
}
