package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.DecompilerMemberReader;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.constant.InvokeDynamicConstants;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerImportSection;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLine;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLineText;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

public final class DecompiledClass {
    private static final int PROGRESSIVE_RENDER_LINE_LIMIT = 50_000;
    private static final int PROGRESSIVE_RENDER_METHOD_LIMIT = 500;
    private final Trinity trinity;
    private final ClassInput classInput;
    private final Map<MethodInput, DecompiledMethod> decompiledMethodMap = new HashMap<>();
    private final List<DecompilerComponent> componentList;
    private final Map<MemberDetails, DecompilerMemberReader.MemberComponents> methodComponents;
    private final Map<MemberDetails, DecompilerMemberReader.MemberComponents> fieldComponents;
    private final Map<Integer, ClassPreview> classPreviewCache = new HashMap<>();
    private final Map<MethodPreviewKey, MethodPreview> methodPreviewCache = new HashMap<>();
    private final Map<MethodUsagePreviewKey, MethodUsagePreview> methodUsagePreviewCache = new HashMap<>();
    private final Map<MemberDetails, List<DecompilerLineText>> fieldPreviewCache = new HashMap<>();
    private final Map<VariablePreviewKey, List<DecompilerLineText>> variablePreviewCache = new HashMap<>();
    private DecompilerImportSection importSection;
    private long layoutVersion;
    private String longestLineText = "";
    private StickyHeaders stickyHeaders;
    private final Set<MemberDetails> progressiveMethods = ConcurrentHashMap.newKeySet();
    private final Queue<MethodOutput> pendingMethodOutputs = new ConcurrentLinkedQueue<>();
    private final Queue<MemberReplacement> pendingMemberReplacements = new ConcurrentLinkedQueue<>();
    private final Set<MemberDetails> queuedMethodOutputs = ConcurrentHashMap.newKeySet();
    private volatile DecompiledClass completedClass;
    private volatile boolean progressive;
    /** Whether individual method completions are cheap enough to merge into the live document. */
    private volatile boolean progressiveMethodUpdatesEnabled;
    /**
     * Lines of the decompiled source code file, each containing a {@link DecompilerLine} which holds the
     * text components to be rendered on that specific line.
     */
    private final List<DecompilerLine> lines = new ArrayList<>();
    /**
     * List of components that are currently highlighted.
     */
    private Set<DecompilerComponent> highlightedComponents;

    public DecompiledClass(Trinity trinity, ClassInput classInput, final String rawOutput) throws IOException {
        this.trinity = trinity;
        this.classInput = classInput;

        final DecompilerMemberReader reader = new DecompilerMemberReader(this, rawOutput);
        this.componentList = new ArrayList<>(reader.getComponentList());
        this.methodComponents = new HashMap<>(reader.getMethodComponents());
        this.fieldComponents = new HashMap<>(reader.getFieldComponents());
        this.progressiveMethodUpdatesEnabled = false;

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
        this.progressive = true;

        this.resetLines();
        this.progressiveMethodUpdatesEnabled = this.classInput.getMethodMap().size()
                <= PROGRESSIVE_RENDER_METHOD_LIMIT
                && this.lines.size() < PROGRESSIVE_RENDER_LINE_LIMIT;
        if (this.progressiveMethodUpdatesEnabled) {
            this.progressiveMethods.addAll(this.methodComponents.keySet());
        }
        this.setComponentHighlighted(null);
    }

    public static DecompiledClass progressive(Trinity trinity, ClassInput classInput) throws IOException {
        return new DecompiledClass(trinity, classInput, ProgressiveDecompilerSource.build(classInput));
    }

    public void queueMethodOutput(MemberDetails method, String rawOutput) {
        if (progressive && progressiveMethodUpdatesEnabled
                && progressiveMethods.contains(method) && queuedMethodOutputs.add(method)) {
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
        progressiveMethodUpdatesEnabled = false;
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

        // The completed model already built its lines and indexes on the decompiler thread. It
        // supersedes any queued progressive placeholders; replaying those first can otherwise
        // rebuild a very large class once per frame long after final output is available.
        DecompiledClass completed = completedClass;
        if (completed != null) {
            this.adoptCompletedClass(completed);
            return true;
        }

        if (progressive && progressiveMethodUpdatesEnabled) {
            // Rebuilding an already enormous progressive document for each arriving method is
            // more expensive than waiting for the final model. The finished result is still
            // produced in the background and adopted immediately above.
            if (lines.size() >= PROGRESSIVE_RENDER_LINE_LIMIT) {
                this.disableProgressiveMethodUpdates();
                return false;
            }
            MethodOutput methodOutput = pendingMethodOutputs.poll();
            if (methodOutput != null) {
                replaceMethod(methodOutput);
                return true;
            }
        }

        return false;
    }

    private void disableProgressiveMethodUpdates() {
        this.progressiveMethodUpdatesEnabled = false;
        this.pendingMethodOutputs.clear();
        this.queuedMethodOutputs.clear();
        this.progressiveMethods.clear();
    }

    private void adoptCompletedClass(DecompiledClass completed) {
        componentList.clear();
        componentList.addAll(completed.componentList);
        decompiledMethodMap.clear();
        decompiledMethodMap.putAll(completed.decompiledMethodMap);
        methodComponents.clear();
        methodComponents.putAll(completed.methodComponents);
        fieldComponents.clear();
        fieldComponents.putAll(completed.fieldComponents);

        lines.clear();
        lines.addAll(completed.lines);
        importSection = completed.importSection;
        longestLineText = completed.longestLineText;
        stickyHeaders = completed.stickyHeaders;
        this.clearPreviewCaches();

        pendingMethodOutputs.clear();
        queuedMethodOutputs.clear();
        progressiveMethods.clear();
        completedClass = null;
        progressive = false;
        progressiveMethodUpdatesEnabled = false;
        layoutVersion++;
        setComponentHighlighted(null);
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

    public DecompilerComponent findInstructionComponent(MethodInput methodInput, AbstractInsnNode targetInstruction) {
        MemberDetails targetMember = getInstructionMember(targetInstruction);
        if (targetMember == null) {
            for (Object constant : getInstructionConstants(targetInstruction)) {
                DecompilerComponent component = findInstructionConstantComponent(
                        methodInput, targetInstruction, constant, 0);
                if (component != null) {
                    return component;
                }
            }
            return null;
        }

        int occurrence = 0;
        boolean targetFound = false;
        for (AbstractInsnNode instruction : methodInput.getInstructions()) {
            if (!targetMember.equals(getInstructionMember(instruction))) {
                continue;
            }
            if (instruction == targetInstruction) {
                targetFound = true;
                break;
            }
            occurrence++;
        }
        if (!targetFound) {
            return null;
        }

        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(methodInput.getDetails());
        if (boundaries == null) {
            return null;
        }
        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return null;
        }

        String targetKey = targetMember.toString();
        for (int i = startIndex; i <= endIndex; i++) {
            DecompilerComponent component = componentList.get(i);
            if (!targetKey.equals(component.memberKey)) {
                continue;
            }
            if (occurrence-- == 0) {
                return component;
            }
        }
        return null;
    }

    private static MemberDetails getInstructionMember(AbstractInsnNode instruction) {
        if (instruction instanceof MethodInsnNode method) {
            return new MemberDetails(method.owner, method.name, method.desc);
        }
        if (instruction instanceof FieldInsnNode field) {
            return new MemberDetails(field.owner, field.name, field.desc);
        }
        return null;
    }

    public void setComponentHighlighted(DecompilerComponent component) {
        if (component == null) {
            this.highlightedComponents = Collections.emptySet();
            return;
        }

        Set<DecompilerComponent> highlighted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (DecompilerComponent otherComponent : componentList) {
            if (otherComponent.isSameKind(component)) highlighted.add(otherComponent);
        }
        this.highlightedComponents = highlighted;
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
        this.clearPreviewCaches();
        this.stickyHeaders = null;
    }

    private void clearPreviewCaches() {
        this.classPreviewCache.clear();
        this.methodPreviewCache.clear();
        this.methodUsagePreviewCache.clear();
        this.fieldPreviewCache.clear();
        this.variablePreviewCache.clear();
    }

    public StickyHeaders getStickyHeaders() {
        if (this.stickyHeaders == null) {
            this.stickyHeaders = createStickyHeaders();
        }
        return this.stickyHeaders;
    }

    private StickyHeaders createStickyHeaders() {
        String className = classInput.getNode().name;
        DecompilerLine classLine = null;
        for (DecompilerComponent component : componentList) {
            if (className.equals(component.memberKey)) {
                if (component.getLayoutFirstLine() != null) {
                    classLine = component.getLayoutFirstLine();
                    break;
                }
            }
        }

        List<StickyMethod> methods = new ArrayList<>();
        for (Map.Entry<MemberDetails, DecompilerMemberReader.MemberComponents> entry : methodComponents.entrySet()) {
            int startComponentIndex = entry.getValue().start().getLayoutIndex();
            int endComponentIndex = entry.getValue().end().getLayoutIndex();
            if (startComponentIndex < 0 || endComponentIndex < startComponentIndex) {
                continue;
            }

            DecompilerLine startLine = this.findFirstComponentLine(startComponentIndex, endComponentIndex);
            DecompilerLine endLine = this.findLastComponentLine(startComponentIndex, endComponentIndex);
            if (startLine == null || endLine == null) continue;

            String memberKey = entry.getKey().toString();
            DecompilerLine signatureLine = null;
            for (int i = startComponentIndex; i <= endComponentIndex; i++) {
                DecompilerComponent component = componentList.get(i);
                if (memberKey.equals(component.memberKey)) {
                    signatureLine = component.getLayoutFirstLine();
                    break;
                }
            }
            if (signatureLine != null) {
                methods.add(new StickyMethod(signatureLine, endLine,
                        signatureLine.getLineNumber() - 1, endLine.getLineNumber() - 1));
            }
        }
        methods.sort(Comparator.comparingInt(StickyMethod::signatureLineIndex));
        return new StickyHeaders(classLine,
                classLine == null ? -1 : classLine.getLineNumber() - 1, List.copyOf(methods));
    }

    private DecompilerLine findFirstComponentLine(int start, int end) {
        for (int index = start; index <= end; index++) {
            DecompilerLine line = componentList.get(index).getLayoutFirstLine();
            if (line != null) return line;
        }
        return null;
    }

    private DecompilerLine findLastComponentLine(int start, int end) {
        for (int index = end; index >= start; index--) {
            DecompilerLine line = componentList.get(index).getLayoutLastLine();
            if (line != null) return line;
        }
        return null;
    }

    public ClassPreview getClassPreview(int maximumLines) {
        if (maximumLines <= 0) {
            return ClassPreview.EMPTY;
        }
        return classPreviewCache.computeIfAbsent(maximumLines, this::createClassPreview);
    }

    private ClassPreview createClassPreview(int maximumLines) {
        String className = classInput.getNode().name;
        int signatureLine = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getComponents().stream().anyMatch(text ->
                    className.equals(text.getComponent().memberKey))) {
                signatureLine = i;
                break;
            }
        }
        if (signatureLine == -1) {
            return ClassPreview.EMPTY;
        }

        List<List<DecompilerLineText>> previewLines = new ArrayList<>();
        for (int i = signatureLine; i < lines.size(); i++) {
            List<DecompilerLineText> previewLine = lines.get(i).getComponents().stream()
                    .filter(text -> !text.getText().isEmpty())
                    .toList();
            if (!previewLine.isEmpty()) {
                previewLines.add(previewLine);
            }
        }
        if (previewLines.isEmpty()) {
            return ClassPreview.EMPTY;
        }

        int lastLine = Math.min(maximumLines, previewLines.size());
        return new ClassPreview(List.copyOf(previewLines.subList(0, lastLine)),
                lastLine < previewLines.size());
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

    public MethodUsagePreview getMethodUsagePreview(MethodInput methodInput,
                                                     AbstractInsnNode instruction,
                                                     int surroundingLines,
                                                     boolean highlightOwnerClass,
                                                     boolean highlightConstant,
                                                     Object constantValue,
                                                     int constantOccurrence) {
        if (instruction == null || surroundingLines < 0) {
            return MethodUsagePreview.EMPTY;
        }
        MethodUsagePreviewKey key = new MethodUsagePreviewKey(
                methodInput.getDetails(), instruction, surroundingLines, highlightOwnerClass,
                highlightConstant, constantValue, constantOccurrence);
        return methodUsagePreviewCache.computeIfAbsent(key,
                ignored -> createMethodUsagePreview(
                        methodInput, instruction, surroundingLines, highlightOwnerClass,
                        highlightConstant, constantValue, constantOccurrence));
    }

    public PatternUsagePreview getMethodPatternUsagePreview(MethodInput methodInput,
                                                             List<AbstractInsnNode> instructions,
                                                             int surroundingLines) {
        if (instructions == null || instructions.isEmpty() || surroundingLines < 0) {
            return PatternUsagePreview.EMPTY;
        }
        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(methodInput.getDetails());
        if (boundaries == null) return PatternUsagePreview.EMPTY;

        Set<DecompilerComponent> highlighted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractInsnNode instruction : instructions) {
            DecompilerComponent component = findInstructionComponent(methodInput, instruction);
            if (component != null) highlighted.add(component);
        }
        if (highlighted.isEmpty()) return PatternUsagePreview.EMPTY;

        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) return PatternUsagePreview.EMPTY;

        Set<DecompilerComponent> methodComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        methodComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));
        List<List<DecompilerLineText>> methodLines = new ArrayList<>();
        int signatureLine = -1;
        int usageLine = -1;
        String methodKey = methodInput.getDetails().toString();
        for (DecompilerLine line : lines) {
            List<DecompilerLineText> previewLine = line.getComponents().stream()
                    .filter(text -> methodComponentSet.contains(text.getComponent()))
                    .filter(text -> !text.getText().isEmpty())
                    .toList();
            if (previewLine.isEmpty()) continue;
            if (signatureLine == -1 && previewLine.stream()
                    .anyMatch(text -> methodKey.equals(text.getComponent().memberKey))) {
                signatureLine = methodLines.size();
            }
            if (usageLine == -1 && previewLine.stream()
                    .anyMatch(text -> highlighted.contains(text.getComponent()))) {
                usageLine = methodLines.size();
            }
            methodLines.add(previewLine);
        }
        if (signatureLine == -1 || usageLine <= signatureLine) return PatternUsagePreview.EMPTY;

        List<List<DecompilerLineText>> body = methodLines.subList(signatureLine + 1, methodLines.size());
        int usageBodyLine = usageLine - signatureLine - 1;
        int firstLine = Math.max(0, usageBodyLine - surroundingLines);
        int lastLine = Math.min(body.size(), usageBodyLine + surroundingLines + 1);
        return new PatternUsagePreview(methodLines.get(signatureLine),
                List.copyOf(body.subList(firstLine, lastLine)), Set.copyOf(highlighted),
                firstLine > 0, lastLine < body.size());
    }

    private MethodUsagePreview createMethodUsagePreview(MethodInput methodInput,
                                                         AbstractInsnNode instruction,
                                                         int surroundingLines,
                                                         boolean highlightOwnerClass,
                                                         boolean highlightConstant,
                                                         Object constantValue,
                                                         int constantOccurrence) {
        DecompilerComponent usageComponent = highlightConstant
                ? findInstructionConstantComponent(
                        methodInput, instruction, constantValue, constantOccurrence)
                : findInstructionComponent(methodInput, instruction);
        DecompilerComponent highlightedComponent = highlightOwnerClass && usageComponent != null
                ? findInstructionOwnerComponent(usageComponent, instruction)
                : usageComponent;
        return createComponentUsagePreview(
                methodInput, usageComponent, highlightedComponent, surroundingLines);
    }

    public MethodUsagePreview getVariableUsagePreview(MethodInput methodInput, int variableIndex,
                                                       int componentOccurrence,
                                                       int surroundingLines) {
        DecompilerComponent usageComponent = findVariableComponent(
                methodInput, variableIndex, componentOccurrence);
        return createComponentUsagePreview(
                methodInput, usageComponent, usageComponent, surroundingLines);
    }

    private MethodUsagePreview createComponentUsagePreview(MethodInput methodInput,
                                                            DecompilerComponent usageComponent,
                                                            DecompilerComponent highlightedComponent,
                                                            int surroundingLines) {
        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(methodInput.getDetails());
        if (usageComponent == null || boundaries == null) {
            return MethodUsagePreview.EMPTY;
        }

        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return MethodUsagePreview.EMPTY;
        }

        Set<DecompilerComponent> methodComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        methodComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));

        List<List<DecompilerLineText>> methodLines = new ArrayList<>();
        int signatureLine = -1;
        int usageLine = -1;
        String methodKey = methodInput.getDetails().toString();
        for (DecompilerLine line : lines) {
            List<DecompilerLineText> previewLine = line.getComponents().stream()
                    .filter(text -> methodComponentSet.contains(text.getComponent()))
                    .filter(text -> !text.getText().isEmpty())
                    .toList();
            if (previewLine.isEmpty()) {
                continue;
            }
            if (signatureLine == -1 && previewLine.stream()
                    .anyMatch(text -> methodKey.equals(text.getComponent().memberKey))) {
                signatureLine = methodLines.size();
            }
            if (previewLine.stream().anyMatch(text -> text.getComponent() == usageComponent)) {
                usageLine = methodLines.size();
            }
            methodLines.add(previewLine);
        }

        if (signatureLine == -1 || usageLine <= signatureLine) {
            return MethodUsagePreview.EMPTY;
        }

        List<List<DecompilerLineText>> bodyLines = methodLines.subList(signatureLine + 1, methodLines.size());
        int usageBodyLine = usageLine - signatureLine - 1;
        int contextLineCount = surroundingLines + 1;
        int firstLine = Math.max(0, usageBodyLine - surroundingLines / 2);
        int lastLine = Math.min(bodyLines.size(), firstLine + contextLineCount);
        firstLine = Math.max(0, lastLine - contextLineCount);

        List<List<DecompilerLineText>> context = List.copyOf(bodyLines.subList(firstLine, lastLine));
        return new MethodUsagePreview(methodLines.get(signatureLine), context, highlightedComponent,
                firstLine > 0, lastLine < bodyLines.size());
    }

    private DecompilerComponent findInstructionConstantComponent(MethodInput methodInput,
                                                                 AbstractInsnNode targetInstruction,
                                                                 Object constantValue,
                                                                 int targetOccurrence) {
        int occurrence = 0;
        boolean targetFound = false;
        for (AbstractInsnNode instruction : methodInput.getInstructions()) {
            List<Object> constants = getInstructionConstants(instruction);
            if (instruction == targetInstruction) {
                int matches = 0;
                for (Object value : constants) {
                    if (constantsEqual(value, constantValue)) matches++;
                }
                targetFound = targetOccurrence >= 0 && targetOccurrence < matches;
                if (targetFound) occurrence += targetOccurrence;
                break;
            }
            for (Object value : constants) {
                if (constantsEqual(value, constantValue)) {
                    occurrence++;
                }
            }
        }
        if (!targetFound) {
            return null;
        }

        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(methodInput.getDetails());
        if (boundaries == null) {
            return null;
        }
        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return null;
        }
        for (int i = startIndex; i <= endIndex; i++) {
            DecompilerComponent component = componentList.get(i);
            if (!component.hasConstantValue()
                    || !constantsEqual(component.getConstantValue(), constantValue)) {
                continue;
            }
            if (occurrence-- == 0) {
                return component;
            }
        }
        return null;
    }

    private static List<Object> getInstructionConstants(AbstractInsnNode instruction) {
        if (instruction instanceof IincInsnNode increment) {
            return List.of(increment.incr);
        }
        if (instruction instanceof LdcInsnNode constant) {
            return List.of(constant.cst);
        }
        if (instruction instanceof InsnNode) {
            int opcode = instruction.getOpcode();
            if (opcode == Opcodes.ACONST_NULL) {
                return Collections.singletonList(null);
            }
            if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
                return List.of(InstructionUtil.decodeConstLoad(opcode));
            }
        }
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            return InvokeDynamicConstants.resolve(dynamic);
        }
        if (instruction instanceof IntInsnNode integer) {
            return List.of(integer.operand);
        }
        if (instruction instanceof MultiANewArrayInsnNode array) {
            return List.of(array.dims);
        }
        return List.of();
    }

    private static boolean constantsEqual(Object first, Object second) {
        if (first instanceof Number firstNumber && second instanceof Number secondNumber) {
            if (first instanceof Float || second instanceof Float) {
                return first instanceof Float && second instanceof Float
                        && Float.compare(firstNumber.floatValue(), secondNumber.floatValue()) == 0;
            }
            if (first instanceof Double || second instanceof Double) {
                return first instanceof Double && second instanceof Double
                        && Double.compare(firstNumber.doubleValue(), secondNumber.doubleValue()) == 0;
            }
            if (first instanceof Long || second instanceof Long) {
                return first instanceof Long && second instanceof Long
                        && firstNumber.longValue() == secondNumber.longValue();
            }
            return firstNumber.longValue() == secondNumber.longValue();
        }
        return Objects.equals(first, second);
    }

    private DecompilerComponent findInstructionOwnerComponent(DecompilerComponent usageComponent,
                                                               AbstractInsnNode instruction) {
        String owner;
        if (instruction instanceof MethodInsnNode method) {
            owner = method.owner;
        } else if (instruction instanceof FieldInsnNode field) {
            owner = field.owner;
        } else {
            return null;
        }

        for (DecompilerLine line : lines) {
            List<DecompilerLineText> lineComponents = line.getComponents();
            int usageIndex = -1;
            for (int i = 0; i < lineComponents.size(); i++) {
                if (lineComponents.get(i).getComponent() == usageComponent) {
                    usageIndex = i;
                    break;
                }
            }
            if (usageIndex == -1) {
                continue;
            }
            for (int distance = 1; distance < lineComponents.size(); distance++) {
                int before = usageIndex - distance;
                if (before >= 0 && owner.equals(lineComponents.get(before).getComponent().memberKey)) {
                    return lineComponents.get(before).getComponent();
                }
                int after = usageIndex + distance;
                if (after < lineComponents.size()
                        && owner.equals(lineComponents.get(after).getComponent().memberKey)) {
                    return lineComponents.get(after).getComponent();
                }
            }
            return null;
        }
        return null;
    }

    public List<DecompilerLineText> getFieldDeclarationPreview(FieldInput fieldInput) {
        return fieldPreviewCache.computeIfAbsent(fieldInput.getDetails(), this::createFieldDeclarationPreview);
    }

    private List<DecompilerLineText> createFieldDeclarationPreview(MemberDetails details) {
        DecompilerMemberReader.MemberComponents boundaries = fieldComponents.get(details);
        if (boundaries == null) {
            return List.of();
        }
        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) {
            return List.of();
        }

        Set<DecompilerComponent> fieldComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        fieldComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));
        for (DecompilerLine line : lines) {
            boolean declarationLine = line.getComponents().stream().anyMatch(text ->
                    fieldComponentSet.contains(text.getComponent())
                            && details.toString().equals(text.getComponent().memberKey));
            if (declarationLine) {
                return line.getComponents().stream()
                        .filter(text -> fieldComponentSet.contains(text.getComponent()))
                        .filter(text -> !text.getText().isEmpty())
                        .toList();
            }
        }
        return List.of();
    }

    public List<DecompilerLineText> getVariableDeclarationPreview(MethodInput methodInput, int variableIndex) {
        VariablePreviewKey key = new VariablePreviewKey(methodInput.getDetails(), variableIndex);
        return variablePreviewCache.computeIfAbsent(key,
                ignored -> createVariableDeclarationPreview(methodInput.getDetails(), variableIndex));
    }

    public List<DecompilerVariableReference> getVariableReferences(MethodInput methodInput,
                                                                    int variableIndex) {
        DecompilerMemberReader.MemberComponents boundaries = methodComponents.get(methodInput.getDetails());
        if (boundaries == null) return List.of();

        int startIndex = componentList.indexOf(boundaries.start());
        int endIndex = componentList.indexOf(boundaries.end());
        if (startIndex < 0 || endIndex < startIndex) return List.of();

        Set<DecompilerComponent> methodComponentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        methodComponentSet.addAll(componentList.subList(startIndex, endIndex + 1));
        String methodKey = methodInput.getDetails().toString();
        DecompilerLine signatureLine = lines.stream()
                .filter(line -> line.getComponents().stream().anyMatch(text ->
                        methodComponentSet.contains(text.getComponent())
                                && methodKey.equals(text.getComponent().memberKey)))
                .findFirst().orElse(null);

        List<DecompilerVariableReference> references = new ArrayList<>();
        int componentOccurrence = 0;
        for (DecompilerLine line : lines) {
            int textOffset = 0;
            String lineText = line.getText();
            for (DecompilerLineText text : line.getComponents()) {
                DecompilerComponent component = text.getComponent();
                DecompilerComponent.VariablePreview variable = component.getPreviewVariable();
                int textEnd = textOffset + text.getText().length();
                if (methodComponentSet.contains(component)
                        && isVariable(variable, methodInput, variableIndex)) {
                    DecompilerVariableAccess access = DecompilerVariableAccessClassifier.classify(
                            lineText, textOffset, textEnd, variable.declaration(), line == signatureLine);
                    if (access != null) {
                        references.add(new DecompilerVariableReference(
                                methodInput, variableIndex, componentOccurrence, access,
                                line.getLineNumber(), lineText,
                                List.copyOf(line.getComponents()), component));
                    }
                    componentOccurrence++;
                }
                textOffset = textEnd;
            }
        }
        return List.copyOf(references);
    }

    public DecompilerComponent findVariableComponent(MethodInput methodInput, int variableIndex,
                                                      int componentOccurrence) {
        int occurrence = 0;
        for (DecompilerLine line : lines) {
            for (DecompilerLineText text : line.getComponents()) {
                DecompilerComponent component = text.getComponent();
                if (!isVariable(component.getPreviewVariable(), methodInput, variableIndex)) continue;
                if (occurrence++ == componentOccurrence) return component;
            }
        }
        return null;
    }

    public DecompilerComponent findVariableDeclarationComponent(MethodInput methodInput,
                                                                 int variableIndex) {
        for (DecompilerLine line : lines) {
            for (DecompilerLineText text : line.getComponents()) {
                DecompilerComponent component = text.getComponent();
                DecompilerComponent.VariablePreview variable = component.getPreviewVariable();
                if (isVariable(variable, methodInput, variableIndex) && variable.declaration()) {
                    return component;
                }
            }
        }
        return null;
    }

    private static boolean isVariable(DecompilerComponent.VariablePreview variable,
                                      MethodInput methodInput, int variableIndex) {
        return variable != null && variable.index() == variableIndex
                && variable.methodInput().getDetails().equals(methodInput.getDetails());
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

        for (int index = 0; index < componentList.size(); index++) {
            componentList.get(index).resetLayoutMetadata(index);
        }
        this.longestLineText = "";
        for (DecompilerLine line : this.lines) {
            int characterOffset = 0;
            for (DecompilerLineText text : line.getComponents()) {
                DecompilerComponent component = text.getComponent();
                component.includeLayoutOccurrence(line, characterOffset);
                characterOffset += text.getText().length();
            }
            String lineText = line.getText();
            if (lineText.length() > this.longestLineText.length()) {
                this.longestLineText = lineText;
            }
        }
        this.importSection = DecompilerImportSection.find(this.lines);
        this.layoutVersion++;
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

    public DecompilerImportSection getImportSection() {
        return importSection;
    }

    public ComponentLocation getComponentLocation(DecompilerComponent component) {
        DecompilerLine line = component.getLayoutFirstLine();
        return line == null ? null
                : new ComponentLocation(line, component.getLayoutCharacterOffset());
    }

    public long getLayoutVersion() {
        return layoutVersion;
    }

    public String getLongestLineText() {
        return longestLineText;
    }

    public List<DecompilerComponent> getComponentList() {
        return componentList;
    }

    public boolean containsComponent(DecompilerComponent component) {
        DecompilerLine line = component.getLayoutFirstLine();
        if (line == null) return false;
        int lineIndex = line.getLineNumber() - 1;
        return lineIndex >= 0 && lineIndex < lines.size() && lines.get(lineIndex) == line;
    }

    private record MethodOutput(MemberDetails method, String rawOutput) {
    }

    private record MemberReplacement(MemberDetails previousDetails, MemberDetails currentDetails,
                                     Object memberNode, boolean method, DecompiledClass refreshedClass) {
    }

    private record MethodPreviewKey(MemberDetails details, int maximumLines) {
    }

    private record MethodUsagePreviewKey(MemberDetails details, AbstractInsnNode instruction,
                                         int surroundingLines, boolean highlightOwnerClass,
                                         boolean highlightConstant, Object constantValue,
                                         int constantOccurrence) {
    }

    private record VariablePreviewKey(MemberDetails method, int variableIndex) {
    }

    public record ClassPreview(List<List<DecompilerLineText>> lines, boolean hasMoreLines) {
        private static final ClassPreview EMPTY = new ClassPreview(List.of(), false);
    }

    public record StickyHeaders(DecompilerLine classLine, int classLineIndex,
                                List<StickyMethod> methods) {
    }

    public record StickyMethod(DecompilerLine signatureLine, DecompilerLine endLine,
                               int signatureLineIndex, int endLineIndex) {
    }

    public record ComponentLocation(DecompilerLine line, int characterOffset) {
    }

    public record MethodPreview(List<List<DecompilerLineText>> lines, boolean skippedLeading,
                                boolean hasMoreLines) {
        private static final MethodPreview EMPTY = new MethodPreview(List.of(), false, false);
    }

    public record MethodUsagePreview(List<DecompilerLineText> signature,
                                     List<List<DecompilerLineText>> lines,
                                     DecompilerComponent usageComponent,
                                     boolean skippedLeading, boolean hasMoreLines) {
        private static final MethodUsagePreview EMPTY = new MethodUsagePreview(
                List.of(), List.of(), null, false, false);
    }

    public record PatternUsagePreview(List<DecompilerLineText> signature,
                                      List<List<DecompilerLineText>> lines,
                                      Set<DecompilerComponent> usageComponents,
                                      boolean skippedLeading, boolean hasMoreLines) {
        private static final PatternUsagePreview EMPTY = new PatternUsagePreview(
                List.of(), List.of(), Set.of(), false, false);
    }
}
