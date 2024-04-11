package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

public class XrefWhereField extends XrefWhere {
    private final FieldInput fieldInput;

    public XrefWhereField(FieldInput fieldInput) {
        super("Field");
        this.fieldInput = fieldInput;
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create().menuItem("Go to field", this::followInDecompiler);
    }

    @Override
    public String getText() {
        return this.fieldInput.getDisplayName().getName();
    }

    @Override
    public void followInDecompiler() {
        Main.getDisplayManager().openDecompilerView(this.fieldInput);
    }
}
