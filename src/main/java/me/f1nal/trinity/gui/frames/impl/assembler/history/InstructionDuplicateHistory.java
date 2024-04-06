package me.f1nal.trinity.gui.frames.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.frames.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.frames.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public class InstructionDuplicateHistory extends AssemblerHistory {
    private final InstructionPosition position;
    private final InstructionComponent newInstruction;

    public InstructionDuplicateHistory(InstructionPosition position, InstructionComponent newInstruction) {
        super(FontAwesomeIcons.DiceTwo);
        this.position = position;
        this.newInstruction = newInstruction;
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_INFORMATION;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Duplicated instruction ", CodeColorScheme.TEXT));
        text.add(new ColoredString("#" + position.getIndex() + " ", CodeColorScheme.DISABLED));
        text.addAll(position.getComponent().asText());
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return newInstruction;
    }

    @Override
    public void undo() {
        this.position.getList().remove(newInstruction);
        this.position.getList().queueIdReset();
    }

    @Override
    public void redo() {

    }
}
