package me.f1nal.trinity.execution.exception;

public class MethodNotPresentException extends Exception {
    public MethodNotPresentException(String owner, String name, String desc) {
        super(owner + "." + name + desc);
    }
}
