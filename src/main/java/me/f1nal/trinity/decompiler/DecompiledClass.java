package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.DecompilerMemberReader;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLine;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLineText;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class DecompiledClass {
    private final Trinity trinity;
    private final ClassInput classInput;
    private final Map<MethodInput, DecompiledMethod> decompiledMethodMap = new HashMap<>();
    private final List<DecompilerComponent> componentList;
    /**
     * Lines of the decompiled source code file, each containing a {@link DecompilerLine} which holds the
     * text components to be rendered on that specific line.
     */
    private final List<DecompilerLine> lines = new ArrayList<>();
    /**
     * List of components that are currently highlighted.
     */
    private List<DecompilerComponent> highlightedComponents;

    public DecompiledClass(Trinity trinity, ClassInput classInput, final String rawOutput) throws IOException {
        this.trinity = trinity;
        this.classInput = classInput;

        final DecompilerMemberReader reader = new DecompilerMemberReader(this, rawOutput);
        this.componentList = reader.getComponentList();

        this.resetLines();
        this.setComponentHighlighted(null);
    }

    public void setComponentHighlighted(DecompilerComponent component) {
        if (component == null) {
            this.highlightedComponents = Collections.emptyList();
            return;
        }

        this.highlightedComponents = componentList.stream().filter(otherComponent -> otherComponent.isSameKind(component)).collect(Collectors.toList());
    }

    public boolean isComponentHighlighted(DecompilerComponent component) {
        return highlightedComponents.contains(component);
    }

    public String getText() {
        StringBuilder output = new StringBuilder();
        List<DecompilerLine> lines = getLines();
        for (DecompilerLine line : lines) {
            output.append(line.getText()).append('\n');
        }
        return output.toString();
    }

    public void resetLines() {
        this.addLines(this.componentList);
    }

    private DecompilerLine newLine() {
        DecompilerLine line = new DecompilerLine(this.lines.size() + 1);
        this.lines.add(line);
        return line;
    }

    private void addLines(List<DecompilerComponent> componentList) {
        this.lines.clear();

        DecompilerLine sourceLine = this.newLine();

        for (DecompilerComponent textComponent : componentList) {
            textComponent.refreshText();

            String text = textComponent.getText();
            String[] split = text.split("\n");
            int lines = 0;

            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lines++;
                }
            }

            for (String line : split) {
                if (line != null && !line.isEmpty()) {
                    sourceLine.addComponent(new DecompilerLineText(line, textComponent));
                }

                if (lines-- > 0) sourceLine = this.newLine();
            }
            for (int i = 0; i < lines; i++) {
                sourceLine = this.newLine();
            }
        }
    }

    public DecompiledMethod getMethod(MethodInput methodInput) {
        return decompiledMethodMap.get(methodInput);
    }

    public DecompiledMethod createMethod(MethodInput methodInput) {
        return decompiledMethodMap.computeIfAbsent(methodInput, DecompiledMethod::new);
    }

    public Collection<DecompiledMethod> getMethods() {
        return decompiledMethodMap.values();
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public List<DecompilerLine> getLines() {
        return lines;
    }

    public boolean containsComponent(DecompilerComponent component) {
        for (DecompilerLine line : lines) {
            for (DecompilerLineText lineComponent : line.getComponents()) {
                if (lineComponent.getComponent().equals(component)) {
                    return true;
                }
            }
        }
        return false;
    }
}
