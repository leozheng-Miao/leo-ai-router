/**
 * Yu AI SDK 基础异常
 *
 */
package com.leo.airouter.sdk.exception;

import lombok.Getter;

/**
 * Yu AI SDK 基础异常类
 */
@Getter
public class LeoAIException extends RuntimeException {

    private final int code;

    public LeoAIException(String message) {
        super(message);
        this.code = -1;
    }

    public LeoAIException(int code, String message) {
        super(message);
        this.code = code;
    }

    public LeoAIException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public LeoAIException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}