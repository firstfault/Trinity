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
    private final Map<MemberDetails, DecompilerMemberReader.MemberComponents> methodComponents;
    private final Map<MemberDetails, DecompilerMemberReader.MemberComponents> fieldComponents;
    private final Map<MethodPreviewKey, MethodPreview> methodPreviewCache = new HashMap<>();
    private final Map<VariablePreviewKey, List<DecompilerLineText>> variablePreviewCache = new HashMap<>();
    private final Set<MemberDetails> progressiveMethods = ConcurrentHashMap.newKeySet();
    private final Queue<MethodOutput> pendingMethodOutputs = new ConcurrentLinkedQueue<>();
    private final Queue<MemberReplacement> pendingMemberReplacements = new ConcurrentLinkedQueue<>();
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
        this.methodComponents = new HashMap<>(reader.getMethodComponents());
        this.fieldComponents = new HashMap<>(reader.getFieldComponents());

        this.resetLines();
        this.setComponentHighlighted(null);
    }

    private DecompiledClass(Trinity trinity, ClassInput classInput, ProgressiveDecompilerSource.Result source) throws IOException {
        this.trinity = trinity;
        this.classInput = classInput;

        DecompilerMemberReader reader = new DecompilerMemberReader(this, source.rawOutput());
        this.componentList = new ArrayList<>(reader.getComponentList());
        this.methodComponents = new HashMap<>(reader.getMethodComponents());
        this.fieldComponents = new HashMap<>(reader.getFieldComponents());
        this.progressiveMethods.addAll(this.methodComponents.keySet());
        this.progressive = true;

        this.resetLines();
        this.setComponentHighlighted(null);
    }

    public static DecompiledClass progressive(Trinity trinity, ClassInput classInput) throws IOException {
        return new DecompiledClass(trinity, classInput, ProgressiveDecompilerSource.build(classInput));
    }

    public void queueMethodOutput(MemberDetails method, String rawOutput) {
        if (progressive && progressiveMethods.contains(method) && queuedMethodOutputs.add(method)) {
            pendingMethodOutputs.add(new MethodOutput(method, rawOutput));
        }
    }

    public void queueMemberReplacement(MemberDetails previousDetails, MemberDetails currentDetails,
                                       Object memberNode, boolean method, DecompiledClass refreshedClass) {
        pendingMemberReplacements.add(new MemberReplacement(
                previousDetails, currentDetails, memberNode, method, refreshedClass));
    }

    public void finishProgressive(DecompiledClass completedClass) {
        this.completedClass = completedClass;
    }

    public void failProgressive() {
        pendingMethodOutputs.clear();
        pendingMemberReplacements.clear();
        queuedMethodOutputs.clear();
        progressiveMethods.clear();
        progressive = false;
    }

    /**
     * Applies at most one completed method or member refresh per frame so source updates do not stall
     * the render thread.
     */
    public boolean applyPendingOutput() {
        MemberReplacement replacement = pendingMemberReplacements.poll();
        if (replacement != null) {
            return replaceMember(replacement);
        }

        if (progressive) {
            MethodOutput methodOutput = pendingMethodOutputs.poll();
            if (methodOutput != null) {
                replaceMethod(methodOutput);
                return true;
            }
        }

        DecompiledClass completed = completedClass;
        if (completed != null) {
            componentList.clear();
            componentList.addAll(completed.componentList);
            decompiledMethodMap.clear();
            decompiledMethodMap.putAll(completed.decompiledMethodMap);
            methodComponents.clear();
            methodComponents.putAll(completed.methodComponents);
            fieldComponents.clear();
            fieldComponents.putAll(completed.fieldComponents);
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
        progressiveMethods.remove(methodOutput.method());
        DecompilerMemberReader.MemberComponents components = methodComponents.get(methodOutput.method());
        if (components == null) {
            return;
        }

        int startIndex = componentList.indexOf(components.start());
        int endIndex = componentList.indexOf(components.end());
        if (startIndex == -1 || endIndex < startIndex) {
            return;
        }

        List<DecompilerComponent> placeholderComponents = new ArrayList<>(
                componentList.subList(startIndex, endIndex + 1));
        Map<MethodInput, DecompiledMethod> previousMethods = removeDecompiledMethods(methodOutput.method());
        componentList.subList(startIndex, endIndex + 1).clear();
        methodComponents.remove(methodOutput.method());

        if (!methodOutput.rawOutput().isEmpty()) {
            try {
                DecompilerMemberReader reader = new DecompilerMemberReader(this, methodOutput.rawOutput());
                componentList.addAll(startIndex, reader.getComponentList());
                methodComponents.putAll(reader.getMethodComponents());
            } catch (IOException exception) {
                exception.printStackTrace();
                componentList.addAll(startIndex, placeholderComponents);
                methodComponents.put(methodOutput.method(), components);
                decompiledMethodMap.putAll(previousMethods);
            }
        }

        resetLines();
        setComponentHighlighted(null);
    }

    private boolean replaceMember(MemberReplacement replacement) {
        Map<MemberDetails, DecompilerMemberReader.MemberComponents> targetComponents =
                replacement.method() ? methodComponents : fieldComponents;
        Map<MemberDetails, DecompilerMemberReader.MemberComponents> sourceComponents =
                replacement.method() ? replacement.refreshedClass().methodComponents
                        : replacement.refreshedClass().fieldComponents;
        Map.Entry<MemberDetails, DecompilerMemberReader.MemberComponents> targetEntry =
                findTargetComponents(targetComponents, replacement.previousDetails(), replacement.memberNode());
        DecompilerMemberReader.MemberComponents target = targetEntry == null ? null : targetEntry.getValue();
        DecompilerMemberReader.MemberComponents source = sourceComponents.get(replacement.currentDetails());
        if (target == null || source == null) {
            return false;
        }

        int targetStart = componentList.indexOf(target.start());
        int targetEnd = componentList.indexOf(target.end());
        int sourceStart = replacement.refreshedClass().componentList.indexOf(source.start());
        int sourceEnd = replacement.refreshedClass().componentList.indexOf(source.end());
        if (targetStart == -1 || targetEnd < targetStart || sourceStart == -1 || sourceEnd < sourceStart) {
            return false;
        }

        List<DecompilerComponent> refreshedComponents = new ArrayList<>(
                replacement.refreshedClass().componentList.subList(sourceStart, sourceEnd + 1));
        componentList.subList(targetStart, targetEnd + 1).clear();
        componentList.addAll(targetStart, refreshedComponents);
        targetComponents.remove(targetEntry.getKey());
        targetComponents.put(replacement.currentDetails(), source);

        if (replacement.method()) {
            removeDecompiledMethods(targetEntry.getKey());
            replacement.refreshedClass().decompiledMethodMap.forEach((input, method) -> {
                if (input.getDetails().equals(replacement.currentDetails())) {
                    decompiledMethodMap.put(input, method);
                }
            });
        }

        resetLines();
        setComponentHighlighted(null);
        return true;
    }

    private Map.Entry<MemberDetails, DecompilerMemberReader.MemberComponents> findTargetComponents(
            Map<MemberDetails, DecompilerMemberReader.MemberComponents> components,
            MemberDetails previousDetails, Object memberNode) {
        DecompilerMemberReader.MemberComponents direct = components.get(previousDetails);
        if (direct != null) {
            return Map.entry(previousDetails, direct);
        }
        for (Map.Entry<MemberDetails, DecompilerMemberReader.MemberComponents> entry : components.entrySet()) {
            int start = componentList.indexOf(entry.getValue().start());
            int end = componentList.indexOf(entry.getValue().end());
            if (start < 0 || end < start) {
                continue;
            }
            for (int i = start; i <= end; i++) {
                DecompilerComponent component = componentList.get(i);
                if (component.input != null && component.input.getNode() == memberNode) {
                    return entry;
                }
            }
        }
        return null;
    }

    private Map<MethodInput, DecompiledMethod> removeDecompiledMethods(MemberDetails details) {
        Map<MethodInput, DecompiledMethod> removed = new HashMap<>();
        decompiledMethodMap.entrySet().removeIf(entry -> {
            if (entry.getKey().getDetails().equals(details)) {
                removed.put(entry.getKey(), entry.getValue());
                return true;
            }
            return false;
        });
        return removed;
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
        this.methodPreviewCache.clear();
        this.variablePreviewCache.clear();
    }

    public MethodPreview getMethodPreview(MethodInput methodInput, int maximumLines) {
        if (maximumLines <= 0) {
            return MethodPreview.EMPTY;
        }
        MemberDetails details = methodInput.getDetails();
        return methodPreviewCache.computeIfAbsent(new MethodPreviewKey(details, maximumLines),
                key -> createMethodPreview(key.details(), key.maximumLines()));
    }

    private MethodPreview createMethodPreview(MemberDetails details, int maximumLines) {
        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(details);
        if (boundaries == null) {
            return MethodPreview.EMPTY;
        }

        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return MethodPreview.EMPTY;
        }

        Set<DecompilerComponent> methodComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        methodComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));

        List<List<DecompilerLineText>> methodLines = new ArrayList<>();
        int signatureLine = -1;
        for (DecompilerLine line : lines) {
            List<DecompilerLineText> previewLine = line.getComponents().stream()
                    .filter(text -> methodComponentSet.contains(text.getComponent()))
                    .filter(text -> !text.getText().isEmpty())
                    .toList();
            if (previewLine.isEmpty()) {
                continue;
            }
            if (signatureLine == -1 && previewLine.stream()
                    .anyMatch(text -> details.toString().equals(text.getComponent().memberKey))) {
                signatureLine = methodLines.size();
            }
            methodLines.add(previewLine);
        }

        if (methodLines.isEmpty()) {
            return MethodPreview.EMPTY;
        }

        boolean skippedLeading = signatureLine >= maximumLines;
        int firstLine = skippedLeading ? Math.max(0, signatureLine - 2) : 0;
        int lastLine = Math.min(methodLines.size(), firstLine + maximumLines);
        return new MethodPreview(List.copyOf(methodLines.subList(firstLine, lastLine)),
                skippedLeading, lastLine < methodLines.size());
    }

    public List<DecompilerLineText> getVariableDeclarationPreview(MethodInput methodInput, int variableIndex) {
        VariablePreviewKey key = new VariablePreviewKey(methodInput.getDetails(), variableIndex);
        return variablePreviewCache.computeIfAbsent(key,
                ignored -> createVariableDeclarationPreview(methodInput.getDetails(), variableIndex));
    }

    private List<DecompilerLineText> createVariableDeclarationPreview(MemberDetails method, int variableIndex) {
        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(method);
        if (boundaries == null) {
            return List.of();
        }

        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return List.of();
        }

        Set<DecompilerComponent> methodComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        methodComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));
        for (DecompilerLine line : lines) {
            boolean declarationLine = line.getComponents().stream().anyMatch(text -> {
                DecompilerComponent component = text.getComponent();
                DecompilerComponent.VariablePreview variable = component.getPreviewVariable();
                return methodComponentSet.contains(component) && variable != null && variable.declaration()
                        && variable.index() == variableIndex
                        && variable.methodInput().getDetails().equals(method);
            });
            if (declarationLine) {
                return line.getComponents().stream()
                        .filter(text -> methodComponentSet.contains(text.getComponent()))
                        .filter(text -> !text.getText().isEmpty())
                        .toList();
            }
        }
        return List.of();
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

    private record MemberReplacement(MemberDetails previousDetails, MemberDetails currentDetails,
                                     Object memberNode, boolean method, DecompiledClass refreshedClass) {
    }

    private record MethodPreviewKey(MemberDetails details, int maximumLines) {
    }

    private record VariablePreviewKey(MemberDetails method, int variableIndex) {
    }

    public record MethodPreview(List<List<DecompilerLineText>> lines, boolean skippedLeading,
                                boolean hasMoreLines) {
        private static final MethodPreview EMPTY = new MethodPreview(List.of(), false, false);
    }
}
