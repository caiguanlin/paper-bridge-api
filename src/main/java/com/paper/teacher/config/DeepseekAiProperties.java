package com.paper.teacher.config;

import com.paper.teacher.config.DeepseekAiProperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.deepseek")
@Getter
@Setter
public class DeepseekAiProperties {
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private double temperature = 0.2;
}
