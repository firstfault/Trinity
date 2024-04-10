package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public class InstructionInsertHistory extends AssemblerHistory {
    private final InstructionPosition position;

    public InstructionInsertHistory(InstructionPosition position) {
        super(FontAwesomeIcons.Paste);
        this.position = position;
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_SUCCESS;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Inserted instruction ", CodeColorScheme.TEXT));
        text.add(new ColoredString("#" + position.getIndex() + " ", CodeColorScheme.DISABLED));
        text.addAll(position.getComponent().asText());
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return position.getComponent();
    }

    @Override
    public void undo() {
        this.position.getList().remove(position.getIndex());
        this.position.getList().queueIdReset();
    }

    @Override
    public void redo() {

    }
}
