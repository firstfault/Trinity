package me.f1nal.trinity.application;

import java.util.Objects;

/** Stable application-layer failure exposed consistently to GUI and transport adapters. */
public final class ApplicationException extends RuntimeException {
    private final Code code;

    public ApplicationException(Code code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ApplicationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
    }

    public Code code() {
        return code;
    }

    public enum Code {
        PROJECT_NOT_LOADED,
        PROJECT_NOT_READY,
        PROJECT_ALREADY_OPEN,
        REVISION_CONFLICT,
        TARGET_NOT_FOUND,
        TARGET_ALREADY_EXISTS,
        INVALID_INPUT,
        UNSUPPORTED_OPERATION,
        IO_FAILURE,
        TIMEOUT,
        INTERNAL_ERROR
    }
}
