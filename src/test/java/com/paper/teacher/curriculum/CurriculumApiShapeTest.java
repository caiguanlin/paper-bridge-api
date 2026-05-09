package com.paper.teacher.curriculum;

import com.paper.teacher.curriculum.dto.CurriculumSearchRequest;
import com.paper.teacher.curriculum.dto.CurriculumUpsertRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumApiShapeTest {
    @Test
    void exposesCrudAndTreeEndpoints() {
        assertThat(endpoint("search").getAnnotation(GetMapping.class)).isNotNull();
        assertThat(endpoint("create").getAnnotation(PostMapping.class)).isNotNull();
        assertThat(endpoint("update").getAnnotation(PutMapping.class)).isNotNull();
        assertThat(endpoint("delete").getAnnotation(DeleteMapping.class)).isNotNull();
        assertThat(endpoint("tree").getAnnotation(GetMapping.class)).isNotNull();
    }

    @Test
    void searchUsesRequestObjectForFilters() {
        Method search = endpoint("search");

        assertThat(search.getParameterTypes()).containsExactly(CurriculumSearchRequest.class);
        assertThat(search.getParameters()[0].getAnnotation(ModelAttribute.class)).isNotNull();
    }

    @Test
    void createAndUpdateUseRequestBody() {
        Method create = endpoint("create");
        Method update = endpoint("update");

        assertThat(create.getParameterTypes()).containsExactly(CurriculumUpsertRequest.class);
        assertThat(create.getParameters()[0].getAnnotation(RequestBody.class)).isNotNull();
        assertThat(update.getParameterTypes()).contains(Long.class, CurriculumUpsertRequest.class);
        assertThat(update.getParameters()[1].getAnnotation(RequestBody.class)).isNotNull();
    }

    private Method endpoint(String name) {
        return Arrays.stream(CurriculumController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
