package me.f1nal.trinity.execution.var;

import me.f1nal.trinity.execution.MethodInput;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Set;

public class VariableTable {
    private final MethodInput method;
    private final BiMap<Integer, Variable> variableMap = HashBiMap.create();

    public VariableTable(MethodInput method) {
        this.method = method;

        if (!this.method.getAccessFlags().isStatic()) {
            // "this" ALOAD variable
            this.variableMap.put(0, new ImmutableVariable(this, "this"));
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

    public int getIndex(Variable variable) {
        return variableMap.inverse().get(variable);
    }
}
