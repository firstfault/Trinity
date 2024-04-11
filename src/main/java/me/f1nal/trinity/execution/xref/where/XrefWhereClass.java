package me.f1nal.trinity.execution.xref.where;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

public class XrefWhereClass extends XrefWhere {
    private final ClassInput classInput;

    public XrefWhereClass(ClassInput classInput) {
        super("Class");
        this.classInput = classInput;
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create().menuItem("Go to class", this::followInDecompiler);
    }

    @Override
    public String getText() {
        return this.classInput.getDisplayName().getName();
    }

    @Override
    public void followInDecompiler() {
        Main.getDisplayManager().openDecompilerView(this.classInput);
    }
}
