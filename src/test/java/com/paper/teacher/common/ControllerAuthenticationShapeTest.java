package com.paper.teacher.common;

import com.paper.teacher.controller.PaperController;
import com.paper.teacher.controller.QuestionController;
import com.paper.teacher.controller.AuthController;
import com.paper.teacher.controller.CurriculumController;
import com.paper.teacher.controller.QuestionTypeTemplateController;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerAuthenticationShapeTest {
    @Test
    void controllersDoNotDeclareCurrentTeacherPerEndpoint() {
        assertThat(controllerParameters())
                .allSatisfy(parameter -> {
                    assertThat(parameter.getType()).isNotEqualTo(CurrentTeacher.class);
                    assertThat(parameter.getAnnotation(AuthenticationPrincipal.class)).isNull();
                });
    }

    private Stream<Parameter> controllerParameters() {
        return Stream.of(
                        AuthController.class,
                        CurriculumController.class,
                        PaperController.class,
                        QuestionController.class,
                        QuestionTypeTemplateController.class
                )
                .flatMap(controller -> Stream.of(controller.getDeclaredMethods()))
                .filter(this::isEndpointMethod)
                .flatMap(method -> Stream.of(method.getParameters()));
    }

    private boolean isEndpointMethod(Method method) {
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getPackageName().equals("org.springframework.web.bind.annotation"));
    }
}
