package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

public class XrefWhereMethod extends XrefWhere {
    private final MethodInput methodInput;

    public XrefWhereMethod(MethodInput methodInput) {
        super("Method");
        this.methodInput = methodInput;
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create().menuItem("Go to method", this::followInDecompiler);
    }

    @Override
    public String getText() {
        return methodInput.getDisplayName().getName();
    }

    @Override
    public void followInDecompiler() {
        Main.getDisplayManager().openDecompilerView(this.methodInput);
    }
}
