package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.assembler.line.MethodOpcodeSource;

import java.util.HashMap;
import java.util.Map;

public class DecompiledMethod {
    private final MethodInput methodInput;
    private final Map<Integer, MethodOpcodeSource> opcodeMap = new HashMap<>();

    public DecompiledMethod(MethodInput methodInput) {
        this.methodInput = methodInput;
    }

    public Map<Integer, MethodOpcodeSource> getOpcodeMap() {
        return opcodeMap;
    }

    public MethodInput getMethodInput() {
        return methodInput;
    }
}
