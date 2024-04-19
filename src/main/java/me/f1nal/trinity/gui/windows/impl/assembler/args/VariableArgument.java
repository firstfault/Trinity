package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredImString;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.gui.windows.impl.assembler.fields.TextField;
import me.f1nal.trinity.theme.CodeColorScheme;

public class VariableArgument extends InstructionOperand {
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
    public InstructionOperand copy() {
        return new VariableArgument(this.variable);
    }
}
