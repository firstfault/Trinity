package me.f1nal.trinity.execution.labels;

import imgui.type.ImString;
import org.objectweb.asm.Label;

public class MethodLabel {
    private final LabelTable table;
    private final ImString name;

    public MethodLabel(LabelTable table, String name) {
        this.table = table;
        this.name = new ImString(name, 32);
    }

    public LabelTable getTable() {
        return table;
    }

    public String getName() {
        return name.get();
    }

    public ImString getNameProperty() {
        return name;
    }

    public Label findOriginal() {
        return table.getOriginal(this);
    }
}
