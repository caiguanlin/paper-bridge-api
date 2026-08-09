package com.paper.teacher.modules.auth.service;

import com.paper.teacher.common.CurrentTeacher;
import com.paper.teacher.modules.auth.entity.TeacherUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {
    private static final String SECRET = "paper-bridge-unit-test-secret-key-at-least-256-bits-long";

    @Test
    void issuedTokenCanBeParsedBackToCurrentTeacher() {
        JwtTokenService service = new JwtTokenService("paper-bridge", 720, SECRET);

        CurrentTeacher teacher = service.parse(service.issue(user(42L, "teacher01")));

        assertEquals(42L, teacher.id());
        assertEquals("teacher01", teacher.username());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = new JwtTokenService("paper-bridge", 720, "another-secret-key-with-enough-length-for-hs256").issue(user(1L, "a"));
        JwtTokenService service = new JwtTokenService("paper-bridge", 720, SECRET);

        assertThrows(SignatureException.class, () -> service.parse(token));
    }

    @Test
    void rejectsTokenIssuedByAnotherIssuer() {
        String token = new JwtTokenService("other-issuer", 720, SECRET).issue(user(1L, "a"));
        JwtTokenService service = new JwtTokenService("paper-bridge", 720, SECRET);

        assertThrows(IncorrectClaimException.class, () -> service.parse(token));
    }

    @Test
    void rejectsExpiredToken() {
        String token = new JwtTokenService("paper-bridge", -60, SECRET).issue(user(1L, "a"));
        JwtTokenService service = new JwtTokenService("paper-bridge", 720, SECRET);

        assertThrows(ExpiredJwtException.class, () -> service.parse(token));
    }

    private static TeacherUser user(Long id, String username) {
        TeacherUser user = new TeacherUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
