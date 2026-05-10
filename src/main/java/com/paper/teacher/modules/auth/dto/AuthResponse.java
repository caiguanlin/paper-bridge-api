package com.paper.teacher.modules.auth.dto;

public record AuthResponse(String token, Long teacherId, String username, String displayName) {
}
