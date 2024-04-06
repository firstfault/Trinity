package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MemberDetails;
import imgui.ImGui;

public class FieldComponent extends AbstractMemberComponent<FieldInput> {
    public FieldComponent(String text, FieldInput field, MemberDetails details) {
        super(text, field, details);
    }

    @Override
    protected void showPopup() {
        if (this.showRenamingItem()) {
            return;
        }
        this.showDetails();
        if (this.getInput() != null && ImGui.menuItem("Go to field")) {
            Main.getDisplayManager().openDecompilerView(this.getInput());
        }
        this.showXref();
        ImGui.endPopup();
    }

    @Override
    protected void rename(String newName) {
        getInput().getOwningClass().getExecution().getTrinity().getRemapper().renameField(this.getInput(), newName);
    }

    @Override
    public boolean isRenameable() {
        return getInput() != null;
    }

    @Override
    public String getText() {
        return getInput() == null ? super.getText() : getInput().getDisplayName();
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.FIELD_REF;
    }
}
