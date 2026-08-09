package com.paper.teacher.modules.auth.service;

import com.paper.teacher.modules.auth.repository.TeacherUserRepository;

import com.paper.teacher.modules.auth.entity.TeacherUser;

import com.paper.teacher.modules.auth.dto.AuthResponse;
import com.paper.teacher.modules.auth.dto.LoginRequest;
import com.paper.teacher.modules.auth.dto.RegisterRequest;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.common.Entities;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TeacherUserRepository teacherUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Entities.check(teacherUserRepository.findByUsername(request.username()) == null, "用户名已存在");

        LocalDateTime now = LocalDateTime.now();
        TeacherUser user = new TeacherUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        teacherUserRepository.insert(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        TeacherUser user = teacherUserRepository.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(TeacherUser user) {
        return new AuthResponse(jwtTokenService.issue(user), user.getId(), user.getUsername(), user.getDisplayName());
    }
}
