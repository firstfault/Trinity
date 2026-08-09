package me.f1nal.trinity.gui.windows.impl.variablexref;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.DecompilerVariableReference;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;
import me.f1nal.trinity.util.SystemUtil;

final class VariableXrefLocation extends XrefWhere {
    private final DecompiledClass decompiledClass;
    private final DecompilerVariableReference reference;

    VariableXrefLocation(DecompiledClass decompiledClass, DecompilerVariableReference reference) {
        super("Variable access");
        this.decompiledClass = decompiledClass;
        this.reference = reference;
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create()
                .menuItem("Go to access", this::followInDecompiler)
                .menuItem("Go to declaration", this::followDeclaration)
                .menuItem("Open assembler", this.reference.methodInput()::openAssembler)
                .separator()
                .menu("Copy...", copy -> copy
                        .menuItem("Variable Name", () -> SystemUtil.copyToClipboard(variableName()))
                        .menuItem("Source Line", () -> SystemUtil.copyToClipboard(sourceLine()))
                        .menuItem("Location", () -> SystemUtil.copyToClipboard(copyLocation())));
    }

    @Override
    public String getText() {
        String source = sourceLine();
        return source.isEmpty() ? Integer.toString(reference.lineNumber())
                : reference.lineNumber() + "   " + source;
    }

    @Override
    public Input<?> getInput() {
        return reference.methodInput();
    }

    @Override
    protected void drawPreview(DecompilerPreviewRenderer renderer, Input<?> input,
                               boolean highlightOwnerClass) {
        renderer.drawVariableUsagePreview(decompiledClass, reference.methodInput(),
                reference.variableIndex(), reference.componentOccurrence());
    }

    @Override
    public void followInDecompiler(NavigationAction action) {
        Main.getDisplayManager().followDecompilerVariable(reference.methodInput(),
                reference.variableIndex(), reference.componentOccurrence(), action);
    }

    private void followDeclaration() {
        Main.getDisplayManager().followDecompilerVariableDeclaration(reference.methodInput(),
                reference.variableIndex(), NavigationAction.FOLLOW_XREF);
    }

    String variableName() {
        Variable variable = reference.methodInput().getVariableTable()
                .getVariable(reference.variableIndex());
        return variable.getName();
    }

    String sourceLine() {
        return reference.lineText().strip();
    }

    private String copyLocation() {
        MethodInput method = reference.methodInput();
        return method.getOwningClass().getDisplayName().getName() + "."
                + method.getDisplayName().getName() + ":" + reference.lineNumber();
    }
}
