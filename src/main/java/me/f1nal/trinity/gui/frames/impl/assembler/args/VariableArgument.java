package me.f1nal.trinity.gui.frames.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredImString;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.gui.frames.impl.assembler.fields.TextField;
import me.f1nal.trinity.theme.CodeColorScheme;

public class VariableArgument extends AbstractInsnArgument {
    private final Variable variable;

    public VariableArgument(Variable variable) {
        this.variable = variable;

        this.getDetailsText().add(new ColoredImString(variable.getNameProperty(), CodeColorScheme.VAR_REF));
        if (variable.isEditable()) {
            this.getFields().add(new TextField("Variable Name", variable.getNameProperty()));
        }
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public AbstractInsnArgument copy() {
        return new VariableArgument(this.variable);
    }
}
