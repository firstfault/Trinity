package me.f1nal.trinity.execution.exception;

public class MissingEntryPointException extends Exception {
    public MissingEntryPointException() {
    }

    public MissingEntryPointException(String message) {
        super(message);
    }
}
