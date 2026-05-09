package com.paper.teacher.auth.dto;

public record AuthResponse(String token, Long teacherId, String username, String displayName) {
}
