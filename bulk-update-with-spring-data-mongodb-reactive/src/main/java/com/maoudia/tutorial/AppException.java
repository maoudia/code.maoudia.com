package com.maoudia.tutorial;

import java.util.function.Supplier;

public class AppException extends RuntimeException {
    private final int errorCode;

    public AppException(String reason) {
        super(reason);
        errorCode = -1;
    }

    public AppException(int code, String reason) {
        super(reason);
        errorCode = code;
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
        errorCode = -1;
    }

    public AppException(String message, int code, Throwable cause) {
        super(message, cause);
        errorCode = code;
    }

    public static Supplier<AppException> toSupplier(String reason) {
        return () -> new AppException(reason);
    }

    public static Supplier<AppException> toSupplier(int code, String reason) {
        return () -> new AppException(code, reason);
    }

    public static Supplier<AppException> toSupplier(String reason, Throwable cause) {
        return () -> new AppException(reason, cause);
    }

    public static Supplier<AppException> toSupplier(String reason, int code, Throwable cause) {
        return () -> new AppException(reason, code, cause);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
