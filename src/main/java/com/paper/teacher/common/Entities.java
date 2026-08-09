package com.paper.teacher.common;

public final class Entities {
    private Entities() {
    }

    public static <T> T require(T entity, String message) {
        if (entity == null) {
            throw new BusinessException(message);
        }
        return entity;
    }

    public static void requireAffected(int affectedRows, String message) {
        check(affectedRows > 0, message);
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
