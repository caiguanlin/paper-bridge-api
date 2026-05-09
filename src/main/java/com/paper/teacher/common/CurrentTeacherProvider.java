package com.paper.teacher.common;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentTeacherProvider {
    public CurrentTeacher current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentTeacher teacher)) {
            throw new AuthenticationCredentialsNotFoundException("Current teacher is not authenticated");
        }
        return teacher;
    }

    public Long id() {
        return current().id();
    }
}
