package com.paper.teacher.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度必须在 6 到 72 个字符之间")
        String password,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 64, message = "显示名称不能超过 64 个字符")
        String displayName
) {
}
