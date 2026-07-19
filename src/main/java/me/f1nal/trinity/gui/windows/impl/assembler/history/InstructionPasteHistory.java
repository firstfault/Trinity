package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionList;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public final class InstructionPasteHistory extends AssemblerHistory {
    private final InstructionList instructions;
    private final List<InstructionComponent> pasted;

    public InstructionPasteHistory(InstructionList instructions, List<InstructionComponent> pasted) {
        super(FontAwesomeIcons.Paste);
        this.instructions = instructions;
        this.pasted = List.copyOf(pasted);
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_SUCCESS;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Pasted " + pasted.size() + " instruction"
                + (pasted.size() == 1 ? "" : "s"), CodeColorScheme.TEXT));
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return pasted.isEmpty() ? null : pasted.get(0);
    }

    @Override
    public void undo() {
        instructions.removeAll(pasted);
        instructions.queueIdReset();
    }

    @Override
    public void redo() {
    }
}
