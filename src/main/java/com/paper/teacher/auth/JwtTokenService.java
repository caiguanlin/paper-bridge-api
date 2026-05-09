package com.paper.teacher.auth;

import com.paper.teacher.common.CurrentTeacher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final String issuer;
    private final long expiresMinutes;
    private final SecretKey secretKey;

    public JwtTokenService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expires-minutes}") long expiresMinutes,
            @Value("${app.jwt.secret}") String secret
    ) {
        this.issuer = issuer;
        this.expiresMinutes = expiresMinutes;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(TeacherUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresMinutes * 60)))
                .signWith(secretKey)
                .compact();
    }

    public CurrentTeacher parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new CurrentTeacher(Long.valueOf(claims.getSubject()), claims.get("username", String.class));
    }
}
