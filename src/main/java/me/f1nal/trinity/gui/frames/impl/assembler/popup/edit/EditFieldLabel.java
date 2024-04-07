package me.f1nal.trinity.gui.frames.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.labels.MethodLabel;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditFieldLabel extends EditFieldText<MethodLabel> {
    private final LabelTable table;

    EditFieldLabel(LabelTable table, Supplier<MethodLabel> getter, Consumer<MethodLabel> setter) {
        super(100, "Label Name", "L0", getter, setter);
        this.table = table;
    }

    @Override
    public void draw() {
        super.draw();

        if (!this.isValidInput()) {
            List<String> labelNames = new ArrayList<>();
            String search = getText().get().toLowerCase();
            for (MethodLabel label : table.getLabels()) {
                String name = label.getName();

                if (search.isEmpty() || name.toLowerCase().contains(search)) {
                    labelNames.add(name);
                }
            }
            if (labelNames.isEmpty()) {
                ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, "No label found");
            } else {
                ImGui.pushStyleColor(ImGuiCol.Text, CodeColorScheme.LABEL);
                ImGui.textWrapped(String.join(" ", labelNames));
                ImGui.popStyleColor();
            }
        }
    }

    @Override
    protected MethodLabel parse(String input) throws InvalidEditInputException {
        MethodLabel label = table.getLabel(input);
        if (label == null) {
            throw new InvalidEditInputException();
        }
        return label;
    }

    @Override
    public void updateField() {
        text.set(get().getName());
    }
}
