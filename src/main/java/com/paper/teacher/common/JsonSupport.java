package com.paper.teacher.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonSupport {
    private final ObjectMapper objectMapper;

    public JsonNode readObjectNode(String json, String errorMessage) {
        JsonNode node = read(json, JsonNode.class, errorMessage);
        if (node == null || !node.isObject()) {
            throw new BusinessException(errorMessage);
        }
        return node;
    }

    public <T> T read(String json, Class<T> type, String errorMessage) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(errorMessage);
        }
    }

    public String write(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(errorMessage + "：" + exception.getMessage());
        }
    }
}
