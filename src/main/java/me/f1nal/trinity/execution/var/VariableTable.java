package me.f1nal.trinity.execution.var;

import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.execution.MethodInput;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Set;

public class VariableTable {
    private final MethodInput method;
    private final BiMap<Integer, Variable> variableMap = HashBiMap.create();
    private final int parameterIndexEnd;

    public VariableTable(MethodInput method) {
        this.method = method;
        this.parameterIndexEnd = getParameterIndexEnd(method.getDetails().getDesc());

        if (!this.method.getAccessFlags().isStatic()) {
            // "this" ALOAD variable
            this.variableMap.put(0, new ImmutableVariable(this, "this"));
        }
    }

    public int getParameterIndexEnd() {
        return parameterIndexEnd;
    }

    private static int getParameterIndexEnd(String desc) {
        try {
            return Arrays.stream(Type.getArgumentTypes(desc)).mapToInt(Type::getSize).sum();
        } catch (Throwable throwable) {
            return 0;
        }
    }

    public MethodInput getMethod() {
        return method;
    }

    public Set<Variable> getVariableMap() {
        return variableMap.values();
    }

    public Variable getVariable(int index) {
        return variableMap.computeIfAbsent(index, i -> {
            if (i >= VarExprent.STACK_BASE) {
                return new ImmutableVariable(this, "stk_" + (i - VarExprent.STACK_BASE));
            }

            final int maxLocals = method.getNode().maxLocals;

//            if (index > maxLocals) {
//                throw new RuntimeException(String.format("Index %s is over maxLocals %s", index, maxLocals));
//            }

            return new Variable(this, "var".concat(String.valueOf(index)));
        });
    }

    public Variable getVariable(String name) {
        for (Variable variable : variableMap.values()) {
            if (variable.getName().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    public Integer getIndex(Variable variable) {
        return variableMap.inverse().get(variable);
    }
}
