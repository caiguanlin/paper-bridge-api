package com.paper.teacher.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoresTest {
    @Test
    void sumSkipsNullValues() {
        assertEquals(0, Scores.sum(Arrays.asList(BigDecimal.ONE, null, BigDecimal.TEN))
                .compareTo(BigDecimal.valueOf(11)));
    }

    @Test
    void sumOfEmptyCollectionIsZero() {
        assertEquals(BigDecimal.ZERO, Scores.sumOf(List.<String>of(), BigDecimal::new));
    }
}
