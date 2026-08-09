package com.paper.teacher.modules.auth.service;


import com.paper.teacher.common.CurrentTeacher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                CurrentTeacher teacher = jwtTokenService.parse(header.substring(7));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(teacher, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                log.warn("Rejected JWT for {} {}: {}: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        exception.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
