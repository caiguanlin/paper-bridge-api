package com.paper.teacher.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.teacher.common.BusinessException;
import com.paper.teacher.modules.auth.dto.AuthResponse;
import com.paper.teacher.modules.auth.dto.LoginRequest;
import com.paper.teacher.modules.auth.dto.RegisterRequest;
import com.paper.teacher.modules.auth.entity.TeacherUser;
import com.paper.teacher.modules.auth.repository.TeacherUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private TeacherUserRepository teacherUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordAndReturnsToken() {
        when(teacherUserRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(jwtTokenService.issue(any())).thenReturn("token-value");

        AuthResponse response = authService.register(new RegisterRequest("teacher01", "secret123", "张老师"));

        ArgumentCaptor<TeacherUser> captor = ArgumentCaptor.forClass(TeacherUser.class);
        verify(teacherUserRepository).insert(captor.capture());
        TeacherUser inserted = captor.getValue();
        assertEquals("teacher01", inserted.getUsername());
        assertEquals("hashed", inserted.getPasswordHash());
        assertEquals("张老师", inserted.getDisplayName());
        assertNotNull(inserted.getCreatedAt());
        assertEquals("token-value", response.token());
        assertEquals("teacher01", response.username());
        assertEquals("张老师", response.displayName());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(teacherUserRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("hashed"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(new RegisterRequest("teacher01", "secret123", "张老师")));

        assertEquals("用户名已存在", exception.getMessage());
        verify(teacherUserRepository, never()).insert(any(TeacherUser.class));
    }

    @Test
    void loginReturnsTokenWhenPasswordMatches() {
        when(teacherUserRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("hashed"));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtTokenService.issue(any())).thenReturn("token-value");

        AuthResponse response = authService.login(new LoginRequest("teacher01", "secret123"));

        assertEquals("token-value", response.token());
        assertEquals(7L, response.teacherId());
    }

    @Test
    void loginRejectsUnknownUsername() {
        when(teacherUserRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("missing", "secret123")));

        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(teacherUserRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("hashed"));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("teacher01", "wrong")));

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(jwtTokenService, never()).issue(any());
    }

    private static TeacherUser existingUser(String passwordHash) {
        TeacherUser user = new TeacherUser();
        user.setId(7L);
        user.setUsername("teacher01");
        user.setPasswordHash(passwordHash);
        user.setDisplayName("张老师");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
