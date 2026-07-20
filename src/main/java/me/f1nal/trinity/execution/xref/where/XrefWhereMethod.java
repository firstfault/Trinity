package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.navigation.NavigationAction;

public class XrefWhereMethod extends XrefWhere {
    private final MethodInput methodInput;

    public XrefWhereMethod(MethodInput methodInput) {
        super("Method");
        this.methodInput = methodInput;
    }

    @Override
    public MethodInput getInput() {
        return methodInput;
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create().menuItem("Go to method", this::followInDecompiler);
    }

    @Override
    public String getText() {
        return methodInput.getOwningClass().getDisplaySimpleName() + "." + methodInput.getDisplayName().getName();
    }

    @Override
    public void followInDecompiler(NavigationAction action) {
        Main.getDisplayManager().followDecompilerView(this.methodInput, action);
    }
}
