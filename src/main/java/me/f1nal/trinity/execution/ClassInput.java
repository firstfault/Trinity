package me.f1nal.trinity.execution;

import me.f1nal.trinity.execution.hierarchy.ClassHierarchy;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.IDisplayNameProvider;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

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
    private final List<String> interfaces = new ArrayList<>();
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
        return methodList.get(this.getMemberKey(name, desc));
    }

    public @Nullable FieldInput getField(String name, String desc) {
        return fieldList.get(this.getMemberKey(name, desc));
    }

    public void addInput(MemberInput<?> input) {
        final MemberDetails details = input.getDetails();
        final String memberKey = this.getMemberKey(details.getName(), details.getDesc());

        if (input instanceof MethodInput) {
            methodList.put(memberKey, (MethodInput) input);
        } else {
            fieldList.put(memberKey, (FieldInput) input);
        }

        memberList.put(input.getDetails(), input);
    }

    @Override
    public void populatePopup(PopupItemBuilder builder) {
//        builder.menuItem("View Hierarchy", () -> Main.getWindowManager().addClosableWindow(new ClassHierarchyWindow(this.getOwningClass().getExecution().getTrinity(), this)));
        super.populatePopup(builder);
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
        return memberList.get(memberDetails);
    }

    /**
     * Gives the full class name.
     * @return Returns {@link ClassNode#name}.
     */
    public String getFullName() {
        return getNode().name;
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
        return this.interfaces;
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
        return this.getFullName();
    }

    private static final Map<String, Function<Input<?>, String>> COPYABLE_ELEMENTS = new LinkedHashMap<>() {{
        put("Full Name", input -> ((ClassInput)input).getFullName());
        put("Name", input -> NameUtil.getSimpleName(((ClassInput)input).getFullName()));
    }};
}
