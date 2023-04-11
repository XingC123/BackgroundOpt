package com.venus.backgroundopt.exception;

/**
 * 实例创建失败异常
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/6
 */
public class InstanceCreateFailedException extends RuntimeException {
    public InstanceCreateFailedException() {
        this("实例创建失败");
    }

    public InstanceCreateFailedException(String message) {
        super(message);
    }

    public InstanceCreateFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstanceCreateFailedException(Throwable cause) {
        super(cause);
    }

    public InstanceCreateFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
