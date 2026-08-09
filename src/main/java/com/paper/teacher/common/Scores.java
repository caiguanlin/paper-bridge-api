package com.paper.teacher.common;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;

public final class Scores {
    private Scores() {
    }

    public static BigDecimal sum(Collection<BigDecimal> scores) {
        return sumOf(scores, Function.identity());
    }

    public static <T> BigDecimal sumOf(Collection<T> items, Function<T, BigDecimal> scoreExtractor) {
        return items.stream()
                .map(scoreExtractor)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
