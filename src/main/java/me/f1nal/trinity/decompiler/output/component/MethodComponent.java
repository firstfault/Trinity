package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.frames.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.frames.impl.assembler.line.Instruction2SourceMapping;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.theme.CodeColorScheme;
import imgui.ImGui;

public final class MethodComponent extends AbstractMemberComponent<MethodInput> {
    private final DecompiledClass decompiledClass;
    private boolean renameClass;
    private boolean superOrThis;

    public MethodComponent(String text, MethodInput methodInput, MemberDetails details, DecompiledClass decompiledClass) {
        super(text, methodInput, details);
        this.decompiledClass = decompiledClass;
        if (text.equals("super") || text.equals("this")) {
            this.superOrThis = true;
        }
        this.renameClass = methodInput != null && methodInput.isInitOrClinit();
    }

    @Override
    protected void showPopup() {
        if (this.showRenamingItem()) {
            return;
        }
        this.showDetails();
        if (this.getInput() != null && ImGui.menuItem("Go to method")) {
            Main.getDisplayManager().openDecompilerView(this.getInput());
        }
        this.showXref();
        if (this.getInput() != null) {
            if (ImGui.menuItem("Assemble", "Z")) {
                Instruction2SourceMapping sourceMapping = new Instruction2SourceMapping(this.decompiledClass);
                Main.getDisplayManager().addClosableWindow(new AssemblerFrame(Main.getTrinity(), this.getInput(), sourceMapping));
            }
        }
        ImGui.endPopup();
    }

    @Override
    public String getName() {
        return this.getInput() == null ? this.getText() : renameClass ? getInput().getOwningClass().getDisplayFullName() : this.getInput().getDisplayName();
    }

    @Override
    protected void rename(String newName) {
        Trinity trinity = getInput().getOwningClass().getExecution().getTrinity();
        if (renameClass) {
            trinity.getRemapper().renameClass(getInput().getOwningClass().getClassTarget(), newName);
        } else {
            trinity.getRemapper().renameMethod(getInput(), newName);
//        trinity.getRemapper().setMethodRemap();
        }
    }

    @Override
    public String getText() {
        return this.superOrThis ? super.getText() : renameClass ? getInput().isClinit() ? super.getText() : getInput().getOwningClass().getDisplaySimpleName() : getInput() != null ? getInput().getDisplayName() : super.getText();
    }

    @Override
    public boolean isRenameable() {
        return getInput() != null;
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.METHOD_REF;
    }
}
