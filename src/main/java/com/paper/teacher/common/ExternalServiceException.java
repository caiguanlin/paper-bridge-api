package com.paper.teacher.common;

/**
 * 依赖的外部服务（如 DeepSeek）调用失败，属于上游故障而非调用方参数问题。
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
