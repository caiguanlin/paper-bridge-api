package com.paper.teacher.modules.auth.service;

import com.paper.teacher.common.CurrentTeacher;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRequestWithValidBearerToken() throws Exception {
        when(jwtTokenService.parse("token-value")).thenReturn(new CurrentTeacher(4L, "teacher01"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertEquals(new CurrentTeacher(4L, "teacher01"), authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void clearsContextWhenTokenIsInvalid() throws Exception {
        when(jwtTokenService.parse("bad-token")).thenThrow(new JwtException("invalid"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void skipsRequestsWithoutBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtTokenService);
    }
}
