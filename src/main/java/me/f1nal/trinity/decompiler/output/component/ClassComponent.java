package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.effect.TooltipEffect;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.frames.impl.EditClassWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.gui.frames.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderClassRef;
import me.f1nal.trinity.util.ModifyPriority;
import imgui.ImGui;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ClassComponent extends AbstractRenameableComponent {
    private final String className;
    private final boolean isImport;
    private final @Nullable ClassInput classInput;
    private final Trinity trinity;

    public ClassComponent(String text, String className, boolean isImport, @Nullable ClassInput classInput, Trinity trinity) {
        super(text);
        this.className = className;
        this.isImport = isImport;
        this.classInput = classInput;
        this.trinity = trinity;
        this.getEffectList().add(new TooltipEffect(() -> ColoredStringBuilder.create().text(CodeColorScheme.CLASS_REF, getClassName()).get()));
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.CLASS_REF;
    }

    @Override
    protected void showPopup() {
        if (showRenamingItem()) {
            return;
        }
        if (ImGui.menuItem("View Xrefs", "X")) {
            Main.getDisplayManager().addClosableWindow(new XrefViewerFrame(new XrefBuilderClassRef(trinity.getExecution().getXrefMap(), className), trinity));
        }
        if (classInput != null) {
            Trinity trinity = this.classInput.getExecution().getTrinity();

            if (ImGui.menuItem("Edit class")) {
                Main.getDisplayManager().addClosableWindow(new EditClassWindow(classInput, trinity));
            }

            if (ImGui.menuItem("Go to class")) {
                Main.getDisplayManager().addClosableWindow(this.classInput.getClassTarget().getDefaultViewer());
            }
        }
        ImGui.endPopup();
    }

    @Override
    public String getName() {
        return getClassName();
    }

    @Override
    protected void rename(String newName) {
        trinity.getRemapper().renameClass(Objects.requireNonNull(this.classInput).getClassTarget(), newName);
    }

    @Override
    public boolean isRenameable() {
        return getClassInput() != null;
    }

    @Override
    protected void notifyRefreshed() {
        this.classInput.notifyModified(ModifyPriority.HIGH);
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    @Override
    public String getText() {
        return isImport || getClassInput() == null ? super.getText() : getClassInput().getDisplaySimpleName();
    }

    public String getClassName() {
        return getClassInput() == null ? className : getClassInput().getDisplayFullName();
    }
}
