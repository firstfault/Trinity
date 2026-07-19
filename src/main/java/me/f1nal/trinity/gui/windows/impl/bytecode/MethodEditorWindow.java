package me.f1nal.trinity.gui.windows.impl.bytecode;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.List;

public final class MethodEditorWindow extends AbstractBytecodeEditorWindow {
    private final ClassInput owner;
    private final MethodInput input;
    private final MethodNode node;
    private final String originalName;
    private final String originalDescriptor;
    private final boolean originallyStatic;
    private final BytecodeEditorSupport.AccessEditor access;
    private final ImString name;
    private final ImString descriptor;
    private final BytecodeEditorSupport.NullableText signature;
    private final BytecodeEditorSupport.StringListEditor exceptions;
    private final ImInt maxStack;
    private final ImInt maxLocals;
    private final ImInt visibleAnnotableParameterCount;
    private final ImInt invisibleAnnotableParameterCount;
    private final List<ParameterState> parameters = new ArrayList<>();
    private final List<TryCatchState> tryCatchBlocks = new ArrayList<>();
    private final List<LocalVariableState> localVariables = new ArrayList<>();
    private final List<LocalVariableAnnotationState> visibleLocalVariableAnnotations = new ArrayList<>();
    private final List<LocalVariableAnnotationState> invisibleLocalVariableAnnotations = new ArrayList<>();
    private final BytecodeEditorSupport.AnnotationListEditor visibleAnnotations;
    private final BytecodeEditorSupport.AnnotationListEditor invisibleAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor visibleTypeAnnotations;
    private final BytecodeEditorSupport.TypeAnnotationListEditor invisibleTypeAnnotations;
    private final BytecodeEditorSupport.ParameterAnnotationEditor visibleParameterAnnotations;
    private final BytecodeEditorSupport.ParameterAnnotationEditor invisibleParameterAnnotations;
    private final BytecodeEditorSupport.OptionalAnnotationValueEditor annotationDefault;
    private final BytecodeEditorSupport.AttributeListEditor attributes;

    public MethodEditorWindow(Trinity trinity, ClassInput owner) {
        this(trinity, owner, null, defaultMethod());
    }

    public MethodEditorWindow(Trinity trinity, MethodInput input) {
        this(trinity, input.getOwningClass(), input, input.getNode());
    }

    private MethodEditorWindow(Trinity trinity, ClassInput owner, MethodInput input, MethodNode node) {
        super(input == null ? "Add Method" : "Edit Method: " + input.getDisplayName().getName() + " (" + input.getInstructions().size() + " instructions)", trinity);
        this.owner = owner;
        this.input = input;
        this.node = node;
        this.originalName = node.name;
        this.originalDescriptor = node.desc;
        this.originallyStatic = (node.access & Opcodes.ACC_STATIC) != 0;
        this.access = new BytecodeEditorSupport.AccessEditor(node.access, AccessFlags.Flag::isMethodFlag);
        this.name = BytecodeEditorSupport.text(node.name);
        this.descriptor = BytecodeEditorSupport.text(node.desc);
        this.signature = new BytecodeEditorSupport.NullableText(node.signature);
        this.exceptions = new BytecodeEditorSupport.StringListEditor("Exceptions", node.exceptions);
        this.maxStack = new ImInt(node.maxStack);
        this.maxLocals = new ImInt(node.maxLocals);
        this.visibleAnnotableParameterCount = new ImInt(node.visibleAnnotableParameterCount);
        this.invisibleAnnotableParameterCount = new ImInt(node.invisibleAnnotableParameterCount);
        if (node.parameters != null) {
            node.parameters.forEach(parameter -> parameters.add(new ParameterState(parameter)));
        }
        if (node.tryCatchBlocks != null) {
            node.tryCatchBlocks.forEach(block -> tryCatchBlocks.add(new TryCatchState(block, node)));
        }
        if (node.localVariables != null) {
            node.localVariables.forEach(variable -> localVariables.add(new LocalVariableState(variable, node)));
        }
        if (node.visibleLocalVariableAnnotations != null) {
            node.visibleLocalVariableAnnotations.forEach(annotation ->
                    visibleLocalVariableAnnotations.add(new LocalVariableAnnotationState(annotation, node)));
        }
        if (node.invisibleLocalVariableAnnotations != null) {
            node.invisibleLocalVariableAnnotations.forEach(annotation ->
                    invisibleLocalVariableAnnotations.add(new LocalVariableAnnotationState(annotation, node)));
        }
        this.visibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Visible annotations", node.visibleAnnotations);
        this.invisibleAnnotations = new BytecodeEditorSupport.AnnotationListEditor("Invisible annotations", node.invisibleAnnotations);
        this.visibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Visible type annotations", node.visibleTypeAnnotations);
        this.invisibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor("Invisible type annotations", node.invisibleTypeAnnotations);
        this.visibleParameterAnnotations = new BytecodeEditorSupport.ParameterAnnotationEditor("Visible parameter annotations", node.visibleParameterAnnotations);
        this.invisibleParameterAnnotations = new BytecodeEditorSupport.ParameterAnnotationEditor("Invisible parameter annotations", node.invisibleParameterAnnotations);
        this.annotationDefault = new BytecodeEditorSupport.OptionalAnnotationValueEditor(node.annotationDefault);
        this.attributes = new BytecodeEditorSupport.AttributeListEditor("Method attributes", node.attrs);
    }

    @Override
    protected void drawEditor() {
        if (ImGui.beginTabBar(getId("MethodTabs"))) {
            if (ImGui.beginTabItem("General")) {
                ImGui.inputText("Name", name);
                ImGui.inputText("Descriptor", descriptor);
                signature.draw("Generic signature");
                ImGui.separator();
                ImGui.text("Access flags");
                access.draw();
                exceptions.draw();
                ImGui.endTabItem();
            }
            if (ImGui.beginTabItem("Annotations")) {
                visibleAnnotations.draw();
                invisibleAnnotations.draw();
                visibleTypeAnnotations.draw();
                invisibleTypeAnnotations.draw();
                annotationDefault.draw("Annotation default");
                ImGui.endTabItem();
            }
            ImGui.endTabBar();
        }
    }

    @Override
    protected String stateFingerprint() {
        return BytecodeEditorSupport.stateFingerprint(
                access, name, descriptor, signature, exceptions,
                maxStack, maxLocals, visibleAnnotableParameterCount, invisibleAnnotableParameterCount,
                parameters, tryCatchBlocks, localVariables, visibleLocalVariableAnnotations,
                invisibleLocalVariableAnnotations, visibleAnnotations, invisibleAnnotations,
                visibleTypeAnnotations, invisibleTypeAnnotations, visibleParameterAnnotations,
                invisibleParameterAnnotations, annotationDefault, attributes);
    }

    private void drawParameters() {
        for (int i = 0; i < parameters.size(); i++) {
            ParameterState parameter = parameters.get(i);
            ImGui.pushID("Parameter" + i);
            ImGui.text("Parameter " + i);
            parameter.name.draw("Name");
            ImGui.inputInt("Access mask", parameter.access);
            ImGui.sameLine();
            ImGui.textDisabled(String.format("0x%04X", parameter.access.get()));
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove parameter metadata")) {
                parameters.remove(i--);
            }
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add parameter metadata")) {
            parameters.add(new ParameterState(new ParameterNode(null, 0)));
        }
    }

    private void drawTryCatchBlocks() {
        if (!ImGui.collapsingHeader("Try/catch blocks (" + tryCatchBlocks.size() + ")###TryCatchBlocks")) {
            return;
        }
        for (int i = 0; i < tryCatchBlocks.size(); i++) {
            TryCatchState block = tryCatchBlocks.get(i);
            ImGui.pushID("TryCatch" + i);
            ImGui.inputInt("Start label instruction", block.start);
            ImGui.inputInt("End label instruction", block.end);
            ImGui.inputInt("Handler label instruction (-1 for none)", block.handler);
            block.type.draw("Exception type");
            block.visibleTypeAnnotations.draw();
            block.invisibleTypeAnnotations.draw();
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove try/catch block")) {
                tryCatchBlocks.remove(i--);
            }
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add try/catch block")) {
            tryCatchBlocks.add(new TryCatchState(null, node));
        }
    }

    private void drawLocalVariables() {
        if (!ImGui.collapsingHeader("Local variables (" + localVariables.size() + ")###LocalVariables")) {
            return;
        }
        for (int i = 0; i < localVariables.size(); i++) {
            LocalVariableState variable = localVariables.get(i);
            ImGui.pushID("LocalVariable" + i);
            ImGui.inputText("Name", variable.name);
            ImGui.inputText("Descriptor", variable.descriptor);
            variable.signature.draw("Generic signature");
            ImGui.inputInt("Local index", variable.index);
            ImGui.inputInt("Start label instruction", variable.start);
            ImGui.inputInt("End label instruction", variable.end);
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove local variable")) {
                localVariables.remove(i--);
            }
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add local variable")) {
            localVariables.add(new LocalVariableState(null, node));
        }
    }

    private void drawLocalVariableAnnotations(String label, List<LocalVariableAnnotationState> annotations) {
        if (!ImGui.collapsingHeader(label + " (" + annotations.size() + ")###LocalVariableAnnotationHeader" + label)) return;
        for (int i = 0; i < annotations.size(); i++) {
            LocalVariableAnnotationState annotation = annotations.get(i);
            ImGui.pushID(label + i);
            ImGui.inputInt("Type reference", annotation.typeRef);
            ImGui.inputText("Type path", annotation.typePath);
            annotation.annotation.draw();
            for (int j = 0; j < annotation.ranges.size(); j++) {
                LocalAnnotationRange range = annotation.ranges.get(j);
                ImGui.pushID("Range" + j);
                ImGui.inputInt("Start label instruction", range.start);
                ImGui.inputInt("End label instruction", range.end);
                ImGui.inputInt("Local index", range.index);
                if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove range")) annotation.ranges.remove(j--);
                ImGui.popID();
            }
            if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add range")) {
                annotation.ranges.add(new LocalAnnotationRange(firstLabel(node), lastLabel(node), 0));
            }
            if (ImGui.smallButton(FontAwesomeIcons.Times + " Remove local-variable annotation")) annotations.remove(i--);
            ImGui.separator();
            ImGui.popID();
        }
        if (ImGui.smallButton(FontAwesomeIcons.Plus + " Add local-variable annotation###AddLocalVariableAnnotation" + label)) {
            annotations.add(new LocalVariableAnnotationState(null, node));
        }
    }

    @Override
    protected void saveChanges() {
        String newName = name.get().trim();
        String newDescriptor = descriptor.get().trim();
        BytecodeEditorSupport.requireMemberName(newName, "Method name", true);
        BytecodeEditorSupport.requireMethodDescriptor(newDescriptor);
        if (("<init>".equals(newName) || "<clinit>".equals(newName))
                && Type.getReturnType(newDescriptor).getSort() != Type.VOID) {
            throw new IllegalArgumentException("Constructors and class initializers must return void");
        }
        if ("<clinit>".equals(newName) && !"()V".equals(newDescriptor)) {
            throw new IllegalArgumentException("A class initializer must use descriptor ()V");
        }
        int parameterCount = Type.getArgumentTypes(newDescriptor).length;
        if (parameters.size() > parameterCount) {
            throw new IllegalArgumentException("MethodParameters has more entries than the method descriptor");
        }
        if (visibleAnnotableParameterCount.get() > parameterCount || invisibleAnnotableParameterCount.get() > parameterCount) {
            throw new IllegalArgumentException("Annotable parameter count exceeds the method descriptor parameter count");
        }
        if (visibleParameterAnnotations.size() > parameterCount || invisibleParameterAnnotations.size() > parameterCount) {
            throw new IllegalArgumentException("Parameter annotation array exceeds the method descriptor parameter count");
        }
        if ((access.get() & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 && node.instructions.size() != 0) {
            throw new IllegalArgumentException("Abstract and native methods cannot contain Code instructions");
        }

        MethodInput conflict = owner.getMethod(newName, newDescriptor);
        if (conflict != null && conflict != input && conflict.getOwningClass() == owner) {
            throw new IllegalArgumentException("A method with this name and descriptor already exists");
        }

        node.access = access.get();
        node.name = newName;
        node.desc = newDescriptor;
        node.signature = signature.get();
        node.exceptions = new ArrayList<>(exceptions.get());
        node.maxStack = Math.max(0, maxStack.get());
        node.maxLocals = Math.max(0, maxLocals.get());
        node.parameters = buildParameters();
        node.tryCatchBlocks = buildTryCatchBlocks();
        node.localVariables = buildLocalVariables();
        node.visibleLocalVariableAnnotations = buildLocalVariableAnnotations(visibleLocalVariableAnnotations);
        node.invisibleLocalVariableAnnotations = buildLocalVariableAnnotations(invisibleLocalVariableAnnotations);
        node.visibleAnnotableParameterCount = Math.max(0, visibleAnnotableParameterCount.get());
        node.invisibleAnnotableParameterCount = Math.max(0, invisibleAnnotableParameterCount.get());
        node.visibleAnnotations = visibleAnnotations.get();
        node.invisibleAnnotations = invisibleAnnotations.get();
        node.visibleTypeAnnotations = visibleTypeAnnotations.get();
        node.invisibleTypeAnnotations = invisibleTypeAnnotations.get();
        node.visibleParameterAnnotations = visibleParameterAnnotations.get();
        node.invisibleParameterAnnotations = invisibleParameterAnnotations.get();
        node.annotationDefault = annotationDefault.get();
        node.attrs = attributes.get();

        if (input == null) {
            owner.addMethod(node);
            trinity.getEventManager().postEvent(new EventClassModified(owner));
        } else {
            MemberDetails previousDetails = input.getDetails();
            MethodInput savedInput = input;
            if (!originalName.equals(newName) || !originalDescriptor.equals(newDescriptor)
                    || originallyStatic != ((node.access & Opcodes.ACC_STATIC) != 0)) {
                savedInput = owner.reindexMethod(input);
            }
            trinity.getEventManager().postEvent(new EventMemberModified(savedInput, previousDetails));
        }
    }

    private List<ParameterNode> buildParameters() {
        if (parameters.isEmpty()) {
            return null;
        }
        List<ParameterNode> output = new ArrayList<>(parameters.size());
        for (ParameterState parameter : parameters) {
            output.add(new ParameterNode(parameter.name.get(), parameter.access.get()));
        }
        return output;
    }

    private List<TryCatchBlockNode> buildTryCatchBlocks() {
        List<TryCatchBlockNode> output = new ArrayList<>(tryCatchBlocks.size());
        for (TryCatchState state : tryCatchBlocks) {
            LabelNode start = requireLabel(state.start.get(), "try/catch start");
            LabelNode end = requireLabel(state.end.get(), "try/catch end");
            LabelNode handler = state.handler.get() < 0 ? null : requireLabel(state.handler.get(), "try/catch handler");
            TryCatchBlockNode block = state.node == null ? new TryCatchBlockNode(start, end, handler, state.type.get()) : state.node;
            block.start = start;
            block.end = end;
            block.handler = handler;
            block.type = state.type.get();
            block.visibleTypeAnnotations = state.visibleTypeAnnotations.get();
            block.invisibleTypeAnnotations = state.invisibleTypeAnnotations.get();
            output.add(block);
        }
        return output;
    }

    private List<LocalVariableNode> buildLocalVariables() {
        if (localVariables.isEmpty()) {
            return null;
        }
        List<LocalVariableNode> output = new ArrayList<>(localVariables.size());
        for (LocalVariableState state : localVariables) {
            BytecodeEditorSupport.requireName(state.name.get(), "Local variable name");
            BytecodeEditorSupport.requireFieldDescriptor(state.descriptor.get());
            LocalVariableNode variable = new LocalVariableNode(state.name.get(), state.descriptor.get(), state.signature.get(),
                    requireLabel(state.start.get(), "local variable start"),
                    requireLabel(state.end.get(), "local variable end"), Math.max(0, state.index.get()));
            output.add(variable);
        }
        return output;
    }

    private List<LocalVariableAnnotationNode> buildLocalVariableAnnotations(List<LocalVariableAnnotationState> states) {
        if (states.isEmpty()) return null;
        List<LocalVariableAnnotationNode> output = new ArrayList<>(states.size());
        for (LocalVariableAnnotationState state : states) {
            List<AnnotationNode> annotations = state.annotation.get();
            if (annotations == null || annotations.isEmpty()) {
                throw new IllegalArgumentException("A local-variable annotation requires a descriptor");
            }
            AnnotationNode annotation = annotations.get(0);
            TypePath path;
            try {
                path = state.typePath.get().isBlank() ? null : TypePath.fromString(state.typePath.get());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid local-variable annotation type path");
            }
            LabelNode[] starts = new LabelNode[state.ranges.size()];
            LabelNode[] ends = new LabelNode[state.ranges.size()];
            int[] indices = new int[state.ranges.size()];
            for (int i = 0; i < state.ranges.size(); i++) {
                LocalAnnotationRange range = state.ranges.get(i);
                starts[i] = requireLabel(range.start.get(), "local-variable annotation start");
                ends[i] = requireLabel(range.end.get(), "local-variable annotation end");
                indices[i] = Math.max(0, range.index.get());
            }
            LocalVariableAnnotationNode built = new LocalVariableAnnotationNode(
                    state.typeRef.get(), path, starts, ends, indices, annotation.desc);
            built.values = annotation.values;
            output.add(built);
        }
        return output;
    }

    private LabelNode requireLabel(int instructionIndex, String label) {
        if (instructionIndex < 0 || instructionIndex >= node.instructions.size()
                || !(node.instructions.get(instructionIndex) instanceof LabelNode labelNode)) {
            throw new IllegalArgumentException(label + " must point to a LabelNode instruction");
        }
        return labelNode;
    }

    private static int labelIndex(MethodNode method, LabelNode label) {
        return label == null ? -1 : method.instructions.indexOf(label);
    }

    private static MethodNode defaultMethod() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "newMethod", "()V", null, null);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 0;
        method.maxLocals = 1;
        return method;
    }

    private static final class ParameterState {
        private final BytecodeEditorSupport.NullableText name;
        private final ImInt access;

        private ParameterState(ParameterNode parameter) {
            this.name = new BytecodeEditorSupport.NullableText(parameter.name);
            this.access = new ImInt(parameter.access);
        }
    }

    private static final class TryCatchState {
        private final TryCatchBlockNode node;
        private final ImInt start;
        private final ImInt end;
        private final ImInt handler;
        private final BytecodeEditorSupport.NullableText type;
        private final BytecodeEditorSupport.TypeAnnotationListEditor visibleTypeAnnotations;
        private final BytecodeEditorSupport.TypeAnnotationListEditor invisibleTypeAnnotations;

        private TryCatchState(TryCatchBlockNode block, MethodNode method) {
            this.node = block;
            this.start = new ImInt(block == null ? firstLabel(method) : labelIndex(method, block.start));
            this.end = new ImInt(block == null ? lastLabel(method) : labelIndex(method, block.end));
            this.handler = new ImInt(block == null ? -1 : labelIndex(method, block.handler));
            this.type = new BytecodeEditorSupport.NullableText(block == null ? "java/lang/Throwable" : block.type);
            this.visibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor(
                    "Visible type annotations", block == null ? null : block.visibleTypeAnnotations);
            this.invisibleTypeAnnotations = new BytecodeEditorSupport.TypeAnnotationListEditor(
                    "Invisible type annotations", block == null ? null : block.invisibleTypeAnnotations);
        }
    }

    private static final class LocalVariableState {
        private final ImString name;
        private final ImString descriptor;
        private final BytecodeEditorSupport.NullableText signature;
        private final ImInt start;
        private final ImInt end;
        private final ImInt index;

        private LocalVariableState(LocalVariableNode variable, MethodNode method) {
            this.name = BytecodeEditorSupport.text(variable == null ? "variable" : variable.name);
            this.descriptor = BytecodeEditorSupport.text(variable == null ? "Ljava/lang/Object;" : variable.desc);
            this.signature = new BytecodeEditorSupport.NullableText(variable == null ? null : variable.signature);
            this.start = new ImInt(variable == null ? firstLabel(method) : labelIndex(method, variable.start));
            this.end = new ImInt(variable == null ? lastLabel(method) : labelIndex(method, variable.end));
            this.index = new ImInt(variable == null ? 0 : variable.index);
        }
    }

    private static final class LocalVariableAnnotationState {
        private final ImInt typeRef;
        private final ImString typePath;
        private final BytecodeEditorSupport.AnnotationListEditor annotation;
        private final List<LocalAnnotationRange> ranges = new ArrayList<>();

        private LocalVariableAnnotationState(LocalVariableAnnotationNode node, MethodNode method) {
            this.typeRef = new ImInt(node == null ? 0 : node.typeRef);
            this.typePath = BytecodeEditorSupport.text(node == null || node.typePath == null ? "" : node.typePath.toString());
            AnnotationNode annotationNode = new AnnotationNode(node == null ? "Lannotation/Local;" : node.desc);
            if (node != null && node.values != null) annotationNode.values = new ArrayList<>(node.values);
            this.annotation = new BytecodeEditorSupport.AnnotationListEditor("Annotation", List.of(annotationNode));
            if (node == null) {
                ranges.add(new LocalAnnotationRange(firstLabel(method), lastLabel(method), 0));
            } else {
                for (int i = 0; i < node.start.size(); i++) {
                    ranges.add(new LocalAnnotationRange(labelIndex(method, node.start.get(i)),
                            labelIndex(method, node.end.get(i)), node.index.get(i)));
                }
            }
        }
    }

    private static final class LocalAnnotationRange {
        private final ImInt start;
        private final ImInt end;
        private final ImInt index;

        private LocalAnnotationRange(int start, int end, int index) {
            this.start = new ImInt(start);
            this.end = new ImInt(end);
            this.index = new ImInt(index);
        }
    }

    private static int firstLabel(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            if (method.instructions.get(i) instanceof LabelNode) return i;
        }
        return -1;
    }

    private static int lastLabel(MethodNode method) {
        for (int i = method.instructions.size() - 1; i >= 0; i--) {
            if (method.instructions.get(i) instanceof LabelNode) return i;
        }
        return -1;
    }
}
