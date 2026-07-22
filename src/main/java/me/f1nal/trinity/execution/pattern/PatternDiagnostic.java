package me.f1nal.trinity.execution.pattern;

public record PatternDiagnostic(int line, int column, Severity severity, String message) {
    public enum Severity {
        ERROR,
        WARNING
    }
}
