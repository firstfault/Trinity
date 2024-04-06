package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.effect.TooltipEffect;
import imgui.ImGui;

public class VariableComponent extends AbstractRenameableComponent {
    private final Variable variable;
    private final int var;
    private final String type;

    public VariableComponent(String text, Variable variable, int var, String type) {
        super(text);
        this.variable = variable;
        this.var = var;
        this.type = type;
        this.addEffect(new TooltipEffect(() -> ColoredStringBuilder.create().text(CodeColorScheme.TEXT, "#" + var).newline().
                text(CodeColorScheme.CLASS_REF, this.type == null ? "unknown var type!" : this.type).get()));
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public String getText() {
        Variable variable = getVariable();
        return variable != null ? variable.getName() : super.getText();
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.VAR_REF;
    }

    @Override
    protected void showPopup() {
        if (showRenamingItem()) {
            return;
        }
        ImGui.endPopup();
    }

    @Override
    public String getName() {
        return getText();
    }

    @Override
    protected void rename(String newName) {
        getVariable().getNameProperty().set(newName);
        getVariable().save();
    }

    @Override
    public boolean isRenameable() {
        return getVariable() != null && getVariable().isEditable();
    }

    @Override
    protected void notifyRefreshed() {
        // Nothing to do here :)
    }
}
