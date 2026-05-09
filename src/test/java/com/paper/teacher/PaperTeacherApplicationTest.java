package com.paper.teacher;

import com.paper.teacher.ai.AiQuestionClient;
import com.paper.teacher.ai.DeepseekAiQuestionClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:paper_bridge;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.issuer=paper-bridge",
        "app.jwt.secret=paper-bridge-local-development-secret-key-change-before-production",
        "app.jwt.expires-minutes=720"
})
class PaperTeacherApplicationTest {
    @Autowired
    private AiQuestionClient aiQuestionClient;

    @Test
    void contextLoads() {
    }

    @Test
    void usesDeepseekAiClientByDefault() {
        Assertions.assertInstanceOf(DeepseekAiQuestionClient.class, aiQuestionClient);
    }
}
