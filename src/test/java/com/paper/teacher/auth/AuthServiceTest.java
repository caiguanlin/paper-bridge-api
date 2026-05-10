package com.paper.teacher.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.paper.teacher.modules.auth.AuthService;
import com.paper.teacher.modules.auth.JwtTokenService;
import com.paper.teacher.modules.auth.TeacherUser;
import com.paper.teacher.modules.auth.TeacherUserRepository;
import com.paper.teacher.modules.auth.dto.LoginRequest;
import com.paper.teacher.modules.auth.dto.RegisterRequest;
import com.paper.teacher.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final TeacherUserRepository repository = mock(TeacherUserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwtTokenService = new JwtTokenService(
            "paper-bridge",
            720,
            "paper-bridge-local-development-secret-key-change-before-production"
    );
    private final AuthService authService = new AuthService(repository, passwordEncoder, jwtTokenService);

    @Test
    void registerRejectsDuplicateUsername() {
        when(repository.selectOne(any(Wrapper.class))).thenReturn(user("teacher", "secret123"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("teacher", "secret123", "王老师")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void loginReturnsTokenForCorrectPassword() {
        when(repository.selectOne(any(Wrapper.class))).thenReturn(user("teacher", "secret123"));

        var response = authService.login(new LoginRequest("teacher", "secret123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.username()).isEqualTo("teacher");
        assertThat(response.displayName()).isEqualTo("王老师");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(repository.selectOne(any(Wrapper.class))).thenReturn(user("teacher", "secret123"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("teacher", "bad-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    private TeacherUser user(String username, String rawPassword) {
        TeacherUser user = new TeacherUser();
        user.setId(1L);
        user.setUsername(username);
        user.setDisplayName("王老师");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }
}
