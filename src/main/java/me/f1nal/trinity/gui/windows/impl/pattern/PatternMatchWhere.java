package me.f1nal.trinity.gui.windows.impl.pattern;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.pattern.InstructionPatternMatch;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;

public final class PatternMatchWhere extends XrefWhereMethodInsn {
    private final InstructionPatternMatch match;
    private final String patternSummary;

    public PatternMatchWhere(InstructionPatternMatch match, String patternSummary) {
        super(match.method(), match.navigationInstruction());
        this.match = match;
        this.patternSummary = patternSummary;
    }

    @Override
    protected void drawPreview(DecompilerPreviewRenderer renderer, Input<?> input,
                               boolean highlightOwnerClass) {
        renderer.drawMethodPatternUsagePreview(match.method(), match.instructions());
    }

    @Override
    public void followInDecompiler(NavigationAction action) {
        Main.getDisplayManager().followDecompilerView(match.method(), match.navigationInstruction(),
                NavigationAction.FOLLOW_PATTERN, patternSummary);
    }

    @Override
    public PopupItemBuilder menuItem() {
        return PopupItemBuilder.create()
                .menuItem("Go to match", this::followInDecompiler)
                .menuItem("Open assembler", match.method()::openAssembler);
    }
}
