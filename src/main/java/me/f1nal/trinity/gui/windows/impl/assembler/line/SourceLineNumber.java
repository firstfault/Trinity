package me.f1nal.trinity.gui.windows.impl.assembler.line;

import java.util.Objects;

public class SourceLineNumber {
    private final int lineNumber;

    public SourceLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceLineNumber that = (SourceLineNumber) o;
        return lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber);
    }
}
