package me.f1nal.trinity.gui.windows.impl.assembler.history;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.windows.impl.assembler.InstructionComponent;
import me.f1nal.trinity.gui.windows.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.windows.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.util.history.Changeable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AssemblerHistory implements IBrowserViewerNode, Changeable {
    private final BrowserViewerNode browserViewerNode;
    private List<ColoredString> text;

    public AssemblerHistory(String icon) {
        this.browserViewerNode = new BrowserViewerNode(icon, this::getColor, () -> "", null);
    }

    @Override
    public IKindType getKind() {
        return XrefKind.LITERAL;
    }

    @Override
    public BrowserViewerNode getBrowserViewerNode() {
        return browserViewerNode;
    }

    @Override
    public boolean matches(String searchTerm) {
        List<String> strings = text.stream().map(ColoredString::getText).collect(Collectors.toList());
        return String.join("", strings).contains(searchTerm);
    }

    protected abstract int getColor();

    protected abstract void createText(List<ColoredString> text);

    public List<ColoredString> getText() {
        if (text == null) {
            this.createText(this.text = new ArrayList<>());
        }
        return text;
    }

    public abstract InstructionComponent getHighlightedComponent();
}
