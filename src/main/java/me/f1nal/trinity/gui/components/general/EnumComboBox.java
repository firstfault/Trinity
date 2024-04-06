package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import imgui.type.ImInt;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.INameable;

public class EnumComboBox<T extends INameable> {
    private final ImInt selection = new ImInt(0);
    private final String label;
    private final String[] items;
    private final T[] types;
    protected final String id = ComponentId.getId(this.getClass());

    public EnumComboBox(String label, T[] type, T selection) {
        this.label = label;
        this.types = type;
        this.items = new String[type.length];
        for (int i = 0; i < this.items.length; i++) {
            if (selection == type[i]) {
                this.selection.set(i);
            }
            this.items[i] = type[i].getName();
        }
    }

    public ImInt getSelection() {
        return selection;
    }

    public String getLabel() {
        return label;
    }

    public String[] getItems() {
        return items;
    }

    public T[] getTypes() {
        return types;
    }

    public EnumComboBox(String label, T[] type) {
        this(label, type, null);
    }

    protected final void drawLabel() {
        ImGui.text(this.label);
    }

    public T draw() {
        this.drawLabel();
        ImGui.combo("###" + this.id, selection, items, 6);
        return this.types[selection.get()];
    }
}
