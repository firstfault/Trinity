package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.assembler.drag.InstructionPosition;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public class InstructionDragHistory extends AssemblerHistory {
    private final InstructionPosition drag;

    public InstructionDragHistory(InstructionPosition drag) {
        super(FontAwesomeIcons.MousePointer);
        this.drag = drag;
    }

    @Override
    protected int getColor() {
        return CodeColorScheme.NOTIFY_INFORMATION;
    }

    @Override
    protected void createText(List<ColoredString> text) {
        text.add(new ColoredString("Moved instruction ", CodeColorScheme.TEXT));
        text.add(new ColoredString("#" + drag.getIndex() + " ", CodeColorScheme.DISABLED));
        text.addAll(drag.getComponent().asText());
        text.add(new ColoredString(" to ", CodeColorScheme.TEXT));
        text.add(new ColoredString("#" + drag.getList().indexOf(drag.getComponent()), CodeColorScheme.DISABLED));
    }

    @Override
    public InstructionComponent getHighlightedComponent() {
        return drag.getComponent();
    }

    @Override
    public void undo() {
        drag.getList().remove(this.drag.getComponent());
        if (this.drag.getIndex() >= drag.getList().size()) {
            drag.getList().add(this.drag.getComponent());
        } else {
            drag.getList().add(this.drag.getIndex(), this.drag.getComponent());
        }
        drag.getList().queueIdReset();
    }

    @Override
    public void redo() {

    }
}
