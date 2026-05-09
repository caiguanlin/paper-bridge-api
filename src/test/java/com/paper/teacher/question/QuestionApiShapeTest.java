package com.paper.teacher.question;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionApiShapeTest {
    @Test
    void searchUsesRequestObjectForFilters() throws Exception {
        Class<?> searchRequestType = Class.forName("com.paper.teacher.question.dto.QuestionSearchRequest");

        assertThat(Arrays.stream(searchRequestType.getRecordComponents()).map(component -> component.getName()))
                .containsExactly(
                        "grade",
                        "publisher",
                        "subject",
                        "volume",
                        "unit",
                        "chapter",
                        "questionType",
                        "difficulty"
                );

        Method controllerSearch = Arrays.stream(QuestionController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("search"))
                .findFirst()
                .orElseThrow();
        assertThat(controllerSearch.getParameterTypes()).containsExactly(searchRequestType);
        assertThat(controllerSearch.getParameters()[0].getAnnotation(ModelAttribute.class)).isNotNull();
        assertThat(controllerSearch.getParameters()[0].getAnnotation(RequestParam.class)).isNull();

        assertThat(QuestionService.class.getDeclaredMethod("search", Long.class, searchRequestType)).isNotNull();
    }
}
