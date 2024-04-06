package me.f1nal.trinity.execution.exception;

public final class ClassExistsException extends Exception {
    public ClassExistsException(String className) {
        super(className);
    }
}
