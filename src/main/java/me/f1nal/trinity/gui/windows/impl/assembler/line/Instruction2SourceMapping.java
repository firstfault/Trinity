package me.f1nal.trinity.gui.windows.impl.assembler.line;

import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.execution.ClassInput;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashMap;
import java.util.Map;

public class Instruction2SourceMapping {
    private final DecompiledClass decompiledClass;
    private final Map<Integer, SourceLineNumber> sourceLineNumberMap = new HashMap<>();
    private final Map<AbstractInsnNode, SourceLineNumber> instruction2source = new HashMap<>();
    private final String className;
    private int lineNumber;

    public Instruction2SourceMapping() {
        this.decompiledClass = null;
        this.className = "";
    }

    public Instruction2SourceMapping(DecompiledClass decompiledClass) {
        this.decompiledClass = decompiledClass;

        this.className = this.getClassName(decompiledClass.getClassInput());
    }

    private SourceLineNumber getSourceLine(int lineNumber) {
        return sourceLineNumberMap.computeIfAbsent(lineNumber, SourceLineNumber::new);
    }

    private static int getNewlineCount(char[] chars) {
        int count = 0;
        for (char c : chars) {
            if (c == '\n') ++count;
        }
        return count;
    }

    private String getClassName(ClassInput classInput) {
        String text = classInput.getDisplayName() + ".java";
        if (text.length() > 26) {
            text = ".." + text.substring(text.length() - 24);
        }
        return text;
    }

    public SourceLineNumber getSourceComponent(AbstractInsnNode instruction) {
        return instruction2source.get(instruction);
    }

    public DecompiledClass getDecompiledClass() {
        return decompiledClass;
    }

    public String getClassName() {
        return this.className;
    }

    public String getClassWithLine(AbstractInsnNode instruction) {
        String lineNumber = this.getLineNumber(instruction);
        return this.getClassName() + ":" + lineNumber;
    }

    private String getLineNumber(AbstractInsnNode instruction) {
        SourceLineNumber sourceLine = getSourceComponent(instruction);
        if (sourceLine == null) {
            return "???";
        }
        return String.valueOf(sourceLine.getLineNumber());
    }
}
