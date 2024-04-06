package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.general.EnumComboBox;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;

import java.util.List;

public class DescribableEnumComboBox<T extends INameable & IDescribable> extends EnumComboBox<T> {
    public DescribableEnumComboBox(String label, T[] type, T selection) {
        super(label, type, selection);
    }

    public DescribableEnumComboBox(String label, T[] type) {
        super(label, type);
    }

    @Override
    public T draw() {
        this.drawLabel();
        T selected = this.getSelected();
        if (ImGui.beginCombo("###" + this.id, selected.getName())) {
            for (T type : this.getTypes()) {
                if (ImGui.selectable(type.getName(), selected == type)) {
                    this.getSelection().set(List.of(this.getTypes()).indexOf(type));
                }
                GuiUtil.tooltip(type.getDescription());
            }
            ImGui.endCombo();
        }
        return selected;
    }

    public T getSelected() {
        return this.getTypes()[this.getSelection().get()];
    }
}
