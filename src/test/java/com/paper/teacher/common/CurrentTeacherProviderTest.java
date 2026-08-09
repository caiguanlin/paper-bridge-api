package com.paper.teacher.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentTeacherProviderTest {
    private final CurrentTeacherProvider provider = new CurrentTeacherProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedTeacher() {
        authenticate(new CurrentTeacher(4L, "teacher01"));

        assertEquals(new CurrentTeacher(4L, "teacher01"), provider.current());
        assertEquals(4L, provider.id());
    }

    @Test
    void failsWhenNoAuthenticationPresent() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, provider::current);
    }

    @Test
    void failsWhenPrincipalIsNotATeacher() {
        authenticate("anonymous");

        assertThrows(AuthenticationCredentialsNotFoundException.class, provider::id);
    }

    private static void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
