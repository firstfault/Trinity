package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionList;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class InstructionClearHistory extends AssemblerHistory {
    private final InstructionList list;
    private final List<InstructionComponent> cleared;

    public InstructionClearHistory(InstructionList list) {
        super(FontAwesomeIcons.TrashAlt);
        this.list = list;
        this.cleared = new ArrayList<>(list);
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_ERROR;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Cleared instructions", CodeColorScheme.TEXT));
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return null;
    }

    @Override
    public void undo() {
        list.addAll(cleared);
        list.queueIdReset();
    }

    @Override
    public void redo() {

    }
}
