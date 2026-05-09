package com.paper.teacher.template;

import com.paper.teacher.template.dto.QuestionTypeTemplateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTemplateApiShapeTest {
    @Test
    void exposesPublicTemplateCrudEndpoints() {
        assertThat(endpoint("list").getAnnotation(GetMapping.class)).isNotNull();
        assertThat(endpoint("detail").getAnnotation(GetMapping.class)).isNotNull();
        assertThat(endpoint("create").getAnnotation(PostMapping.class)).isNotNull();
        assertThat(endpoint("update").getAnnotation(PutMapping.class)).isNotNull();
        assertThat(endpoint("delete").getAnnotation(DeleteMapping.class)).isNotNull();
    }

    @Test
    void createAndUpdateUseTemplateRequestBody() {
        Method create = endpoint("create");
        Method update = endpoint("update");

        assertThat(create.getParameterTypes()).containsExactly(QuestionTypeTemplateRequest.class);
        assertThat(create.getParameters()[0].getAnnotation(RequestBody.class)).isNotNull();
        assertThat(update.getParameterTypes()).contains(Long.class, QuestionTypeTemplateRequest.class);
        assertThat(update.getParameters()[1].getAnnotation(RequestBody.class)).isNotNull();
    }

    private Method endpoint(String name) {
        return Arrays.stream(QuestionTypeTemplateController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
