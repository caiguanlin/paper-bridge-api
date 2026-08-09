package com.paper.teacher.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSupportTest {
    private final JsonSupport jsonSupport = new JsonSupport(new ObjectMapper());

    @Test
    void readObjectNodeRejectsNonObjectJson() {
        assertTrue(jsonSupport.readObjectNode("{\"a\":1}", "bad").isObject());
        assertEquals("bad", assertThrows(BusinessException.class,
                () -> jsonSupport.readObjectNode("[1,2]", "bad")).getMessage());
        assertEquals("bad", assertThrows(BusinessException.class,
                () -> jsonSupport.readObjectNode("not json", "bad")).getMessage());
    }

    @Test
    void writeWrapsSerializationFailures() {
        assertEquals("[1,2]", jsonSupport.write(List.of(1, 2), "failed"));
        assertTrue(assertThrows(BusinessException.class,
                () -> jsonSupport.write(new Object(), "failed")).getMessage().startsWith("failed"));
    }
}
