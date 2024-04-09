package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public class InstructionDeleteHistory extends AssemblerHistory {
    private final InstructionPosition position;

    public InstructionDeleteHistory(InstructionPosition position) {
        super(FontAwesomeIcons.Trash);
        this.position = position;
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_ERROR;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Deleted instruction ", CodeColorScheme.TEXT));
        text.add(new ColoredString("#" + position.getIndex() + " ", CodeColorScheme.DISABLED));
        text.addAll(position.getComponent().asText());
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return null;
    }

    @Override
    public void undo() {
        this.position.getList().add(position.getIndex(), position.getComponent());
        this.position.getList().queueIdReset();
    }

    @Override
    public void redo() {

    }
}
