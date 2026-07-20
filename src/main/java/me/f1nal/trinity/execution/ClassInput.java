package me.f1nal.trinity.execution;

import me.f1nal.trinity.execution.hierarchy.ClassHierarchy;
import me.f1nal.trinity.execution.hierarchy.MemberResolver;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.bytecode.BytecodeEditorLauncher;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.IDisplayNameProvider;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Function;

public final class ClassInput extends Input<ClassNode> implements IDisplayNameProvider {
    /**
     * Program execution flow.
     */
    private final Execution execution;
    /**
     * Map of methods that are referenced and will be compiled in the output.
     */
    private final Map<String, MethodInput> methodList = new LinkedHashMap<>();
    private final Map<String, FieldInput> fieldList = new LinkedHashMap<>();
    private final Map<MemberDetails, MemberInput<?>> memberList = new HashMap<>();
    private final ClassTarget classTarget;
    private final ClassHierarchy classHierarchy = new ClassHierarchy(this);

    public ClassInput(Execution execution, ClassNode classNode, ClassTarget classTarget) {
        super(classNode);
        this.execution = execution;
        this.classTarget = classTarget;
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public ClassTarget getClassTarget() {
        return classTarget;
    }

    /**
     * Gives a key that can be used to query {@link ClassInput#methodList}
     */
    private String getMemberKey(String name, String desc) {
        return name + desc;
    }

    public @Nullable MethodInput getMethod(String name, String desc) {
        MethodInput declaredMethod = getDeclaredMethod(name, desc);
        return declaredMethod != null ? declaredMethod : MemberResolver.resolveMethod(this, name, desc);
    }

    public @Nullable FieldInput getField(String name, String desc) {
        FieldInput declaredField = getDeclaredField(name, desc);
        return declaredField != null ? declaredField : MemberResolver.resolveField(this, name, desc);
    }

    public @Nullable MethodInput getDeclaredMethod(String name, String desc) {
        return methodList.get(this.getMemberKey(name, desc));
    }

    public @Nullable FieldInput getDeclaredField(String name, String desc) {
        return fieldList.get(this.getMemberKey(name, desc));
    }

    public void addInput(MemberInput<?> input) {
        addInput(input.getDetails(), input);
    }

    public MethodInput addMethod(MethodNode method) {
        if (getNode().methods.stream().anyMatch(existing -> existing.name.equals(method.name) && existing.desc.equals(method.desc))) {
            throw new IllegalArgumentException("Method already exists: " + method.name + method.desc);
        }
        if (!getNode().methods.contains(method)) {
            getNode().methods.add(method);
        }
        MethodInput input = new MethodInput(method, this);
        addInput(input);
        return input;
    }

    public FieldInput addField(org.objectweb.asm.tree.FieldNode field) {
        if (getNode().fields.stream().anyMatch(existing -> existing.name.equals(field.name) && existing.desc.equals(field.desc))) {
            throw new IllegalArgumentException("Field already exists: " + field.name + " " + field.desc);
        }
        if (!getNode().fields.contains(field)) {
            getNode().fields.add(field);
        }
        FieldInput input = new FieldInput(field, this);
        addInput(input);
        return input;
    }

    public MethodInput reindexMethod(MethodInput input) {
        removeInput(input);
        MethodInput replacement = new MethodInput(input.getNode(), this);
        preserveDisplayName(input, replacement);
        preserveMethodState(input, replacement);
        addInput(replacement);
        return replacement;
    }

    private static void preserveMethodState(MethodInput input, MethodInput replacement) {
        input.getVariableTable().getVariableMap().forEach(variable -> {
            Integer index = variable.findIndex();
            if (index != null && variable.isEditable()) {
                replacement.getVariableTable().getVariable(index).setName(variable.getName());
            }
        });
        input.getLabelTable().getLabels().forEach(label -> {
            org.objectweb.asm.Label original = label.findOriginal();
            if (original != null) {
                replacement.getLabelTable().getLabel(original).getNameProperty().set(label.getName());
            }
        });
    }

    public FieldInput reindexField(FieldInput input) {
        removeInput(input);
        FieldInput replacement = new FieldInput(input.getNode(), this);
        preserveDisplayName(input, replacement);
        addInput(replacement);
        return replacement;
    }

    public void reindexDeclaredMembers() {
        Map<org.objectweb.asm.tree.MethodNode, MethodInput> methodsByNode = new IdentityHashMap<>();
        Map<org.objectweb.asm.tree.FieldNode, FieldInput> fieldsByNode = new IdentityHashMap<>();
        methodList.values().forEach(method -> methodsByNode.put(method.getNode(), method));
        fieldList.values().forEach(field -> fieldsByNode.put(field.getNode(), field));
        methodList.clear();
        fieldList.clear();
        memberList.clear();
        getNode().methods.forEach(method -> {
            MethodInput replacement = new MethodInput(method, this);
            MethodInput source = methodsByNode.get(method);
            if (source != null) {
                preserveDisplayName(source, replacement);
                preserveMethodState(source, replacement);
            }
            addInput(replacement);
        });
        getNode().fields.forEach(field -> {
            FieldInput replacement = new FieldInput(field, this);
            FieldInput source = fieldsByNode.get(field);
            if (source != null) preserveDisplayName(source, replacement);
            addInput(replacement);
        });
    }

    private void removeInput(MemberInput<?> input) {
        methodList.entrySet().removeIf(entry -> entry.getValue() == input);
        fieldList.entrySet().removeIf(entry -> entry.getValue() == input);
        memberList.entrySet().removeIf(entry -> entry.getValue() == input);
    }

    private static void preserveDisplayName(MemberInput<?> source, MemberInput<?> target) {
        if (source.getDisplayName().getType() != me.f1nal.trinity.remap.RenameType.NONE) {
            target.getDisplayName().setName(source.getDisplayName().getName(), source.getDisplayName().getType());
        }
    }

    private void addInput(MemberDetails query, MemberInput<?> input) {
        final MemberDetails details = input.getDetails();
        final String memberKey = this.getMemberKey(details.getName(), details.getDesc());

        if (input instanceof MethodInput) {
            methodList.put(memberKey, (MethodInput) input);
        } else {
            fieldList.put(memberKey, (FieldInput) input);
        }

        memberList.put(query, input);
    }

    @Override
    public void populatePopup(PopupItemBuilder builder) {
//        builder.menuItem("View Hierarchy", () -> Main.getWindowManager().addClosableWindow(new ClassHierarchyWindow(this.getOwningClass().getExecution().getTrinity(), this)));
        super.populatePopup(builder);
        builder.separator()
                .menuItem("Add Field", () -> BytecodeEditorLauncher.addField(this))
                .menuItem("Add Method", () -> BytecodeEditorLauncher.addMethod(this));
    }

    @Override
    public DisplayName getDisplayName() {
        return classTarget.getDisplayName();
    }

    public Map<String, MethodInput> getMethodMap() {
        return methodList;
    }

    public Map<String, FieldInput> getFieldMap() {
        return fieldList;
    }

    public Collection<MemberInput<?>> getMemberList() {
        return memberList.values();
    }

    public MemberInput<?> getMember(MemberDetails memberDetails) {
        MemberInput<?> declaredMember = memberList.get(memberDetails);
        if (declaredMember != null) {
            return declaredMember;
        }
        return memberDetails.getDesc().startsWith("(")
                ? getMethod(memberDetails.getName(), memberDetails.getDesc())
                : getField(memberDetails.getName(), memberDetails.getDesc());
    }

    /**
     * Gives the full class name.
     * @return Returns {@link ClassNode#name}.
     */
    public String getRealName() {
        return getNode().name;
    }

    @Override
    public String getFullName() {
        return getDisplayName().getName();
    }

    @Override
    public InputType getType() {
        return InputType.CLASS;
    }

    public String getDisplaySimpleName() {
        return NameUtil.getSimpleName(this.getDisplayName().getName());
    }

    /**
     * Gives the full super class name.
     * @return Returns {@link ClassNode#superName}.
     */
    public String getSuperName() {
        return getNode().superName;
    }

    public List<String> getInterfaces() {
        return Objects.requireNonNullElse(getNode().interfaces, Collections.emptyList());
    }

    public Execution getExecution() {
        return execution;
    }

    public boolean mayHaveAnotherMethod() {
        return getNode().methods.size() < 65535;
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        this.getNode().access = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return this.getNode().access;
    }

    public boolean isInterface() {
        return (this.getAccessFlagsMask() & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isFinal() {
        return (this.getAccessFlagsMask() & Opcodes.ACC_FINAL) != 0;
    }

    @Override
    public boolean isAccessFlagValid(AccessFlags.Flag flag) {
        return flag.isClassFlag();
    }

    @Override
    public void rename(Remapper remapper, String newName) {
        remapper.renameClass(this.getClassTarget(), newName);
    }

    @Override
    public Map<String, Function<Input<?>, String>> getCopyableElements() {
        return COPYABLE_ELEMENTS;
    }

    @Override
    public XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return getClassTarget().createXrefBuilder(xrefMap);
    }

    @Override
    public ClassInput getOwningClass() {
        return this;
    }

    @Override
    public String toString() {
        return this.getRealName();
    }

    private static final Map<String, Function<Input<?>, String>> COPYABLE_ELEMENTS = new LinkedHashMap<>() {{
        put("Full Name", input -> ((ClassInput)input).getRealName());
        put("Name", input -> NameUtil.getSimpleName(((ClassInput)input).getRealName()));
    }};
}
