package me.f1nal.trinity.execution.pattern;

import java.util.List;

/** A validated, immutable instruction search pattern. */
public final class InstructionPattern {
    private final String source;
    private final boolean includeMetadata;
    final List<Element> elements;

    InstructionPattern(String source, boolean includeMetadata, List<Element> elements) {
        this.source = source;
        this.includeMetadata = includeMetadata;
        this.elements = List.copyOf(elements);
    }

    public String source() {
        return source;
    }

    public boolean includeMetadata() {
        return includeMetadata;
    }

    public int instructionPatternCount() {
        return (int) elements.stream().filter(element -> !(element instanceof Gap)).count();
    }

    sealed interface Element permits Gap, AnyInstruction, InstructionLine {
    }

    enum Gap implements Element {
        INSTANCE
    }

    enum AnyInstruction implements Element {
        INSTANCE
    }

    record InstructionLine(int sourceLine, String opcode,
                           List<InstructionPatternCompiler.OperandMatcher> operands) implements Element {
    }
}
