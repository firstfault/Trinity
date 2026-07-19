package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.DecompilerMemberReader;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLine;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLineText;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DecompiledClass {
    private final Trinity trinity;
    private final ClassInput classInput;
    private final Map<MethodInput, DecompiledMethod> decompiledMethodMap = new HashMap<>();
    private final List<DecompilerComponent> componentList;
    private final Map<MemberDetails, DecompilerMemberReader.MethodComponents> progressiveMethods;
    private final Queue<MethodOutput> pendingMethodOutputs = new ConcurrentLinkedQueue<>();
    private final Set<MemberDetails> queuedMethodOutputs = ConcurrentHashMap.newKeySet();
    private volatile DecompiledClass completedClass;
    private volatile boolean progressive;
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
        this.componentList = new ArrayList<>(reader.getComponentList());
        this.progressiveMethods = new ConcurrentHashMap<>();

        this.resetLines();
        this.setComponentHighlighted(null);
    }

    private DecompiledClass(Trinity trinity, ClassInput classInput, ProgressiveDecompilerSource.Result source) throws IOException {
        this.trinity = trinity;
        this.classInput = classInput;

        DecompilerMemberReader reader = new DecompilerMemberReader(this, source.rawOutput());
        this.componentList = new ArrayList<>(reader.getComponentList());
        this.progressiveMethods = new ConcurrentHashMap<>(reader.getMethodComponents());
        this.progressive = true;

        this.resetLines();
        this.setComponentHighlighted(null);
    }

    public static DecompiledClass progressive(Trinity trinity, ClassInput classInput) throws IOException {
        return new DecompiledClass(trinity, classInput, ProgressiveDecompilerSource.build(classInput));
    }

    public void queueMethodOutput(MemberDetails method, String rawOutput) {
        if (progressive && progressiveMethods.containsKey(method) && queuedMethodOutputs.add(method)) {
            pendingMethodOutputs.add(new MethodOutput(method, rawOutput));
        }
    }

    public void finishProgressive(DecompiledClass completedClass) {
        this.completedClass = completedClass;
    }

    public void failProgressive() {
        pendingMethodOutputs.clear();
        queuedMethodOutputs.clear();
        progressive = false;
    }

    /**
     * Applies at most one completed method per frame so large classes become
     * visible progressively without stalling the render thread.
     */
    public boolean applyPendingMethodOutput() {
        if (!progressive) {
            return false;
        }

        MethodOutput methodOutput = pendingMethodOutputs.poll();
        if (methodOutput != null) {
            replaceMethod(methodOutput);
            return true;
        }

        DecompiledClass completed = completedClass;
        if (completed != null) {
            componentList.clear();
            componentList.addAll(completed.componentList);
            decompiledMethodMap.clear();
            decompiledMethodMap.putAll(completed.decompiledMethodMap);
            progressiveMethods.clear();
            completedClass = null;
            progressive = false;
            resetLines();
            setComponentHighlighted(null);
            return true;
        }
        return false;
    }

    private void replaceMethod(MethodOutput methodOutput) {
        queuedMethodOutputs.remove(methodOutput.method());
        DecompilerMemberReader.MethodComponents methodComponents = progressiveMethods.get(methodOutput.method());
        if (methodComponents == null) {
            return;
        }

        int startIndex = componentList.indexOf(methodComponents.start());
        int endIndex = componentList.indexOf(methodComponents.end());
        if (startIndex == -1 || endIndex < startIndex) {
            return;
        }

        progressiveMethods.remove(methodOutput.method());
        List<DecompilerComponent> placeholderComponents = new ArrayList<>(
                componentList.subList(startIndex, endIndex + 1));
        componentList.subList(startIndex, endIndex + 1).clear();

        if (!methodOutput.rawOutput().isEmpty()) {
            try {
                DecompilerMemberReader reader = new DecompilerMemberReader(this, methodOutput.rawOutput());
                componentList.addAll(startIndex, reader.getComponentList());
            } catch (IOException exception) {
                exception.printStackTrace();
                componentList.addAll(startIndex, placeholderComponents);
                progressiveMethods.put(methodOutput.method(), methodComponents);
            }
        }

        resetLines();
        setComponentHighlighted(null);
    }

    public boolean isProgressive() {
        return progressive;
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
                if (textComponent.hasCustomRenderer() || line != null && !line.isEmpty()) {
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

    public List<DecompilerComponent> getComponentList() {
        return componentList;
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

    private record MethodOutput(MemberDetails method, String rawOutput) {
    }
}
