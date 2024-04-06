package me.f1nal.trinity.execution.var;

public class ImmutableVariable extends Variable {
    public ImmutableVariable(VariableTable table, String name) {
        super(table, name);
    }

    @Override
    public boolean isEditable() {
        return false;
    }
}
