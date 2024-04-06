package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.execution.var.VariableTable;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EditFieldVariable extends EditFieldText<Variable> {
    private final VariableTable table;

    EditFieldVariable(VariableTable table, Consumer<Variable> setter) {
        super(100, "Variable", "var0", setter);
        this.table = table;
    }

    @Override
    public void draw() {
        super.draw();

        if (!this.isValidInput()) {
            List<String> variableNames = new ArrayList<>();
            String search = getText().get().toLowerCase();
            for (Variable variable : table.getVariableMap()) {
                String name = variable.getName();

                if (search.isEmpty() || name.toLowerCase().contains(search)) {
                    variableNames.add(name);
                }
            }
            if (variableNames.isEmpty()) {
                ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, "No variable found");
            } else {
                ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.VAR_REF);
                ImGui.textWrapped(String.join(" ", variableNames));
                ImGui.popStyleColor();
            }
        }
    }

    @Override
    protected Variable parse(String input) throws InvalidEditInputException {
        Variable variable = table.getVariable(input);
        if (variable == null) {
            throw new InvalidEditInputException();
        }
        return variable;
    }

    public VariableTable getTable() {
        return table;
    }
}
