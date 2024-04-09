package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.execution.xref.ClassXref;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.frames.impl.classes.classhierarchy.ClassHierarchyWindow;
import me.f1nal.trinity.gui.frames.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderClassRef;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.ModifyNotifiable;
import me.f1nal.trinity.util.ModifyPriority;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public final class ClassInput extends Input implements ModifyNotifiable {
    /**
     * Program execution flow.
     */
    private final Execution execution;
    /**
     * {@link ClassNode} object that this instance is referencing.
     */
    private final ClassNode classNode;
    /**
     * Map of methods that are referenced and will be compiled in the output.
     */
    private final Map<String, MethodInput> methodList = new LinkedHashMap<>();
    private final Map<String, FieldInput> fieldList = new LinkedHashMap<>();
    private final List<String> interfaces = new ArrayList<>();
    private final List<ClassInput> superClasses = new ArrayList<>();
    private final AccessFlags accessFlags = new AccessFlags(this, this);
    private boolean areSuperclassesKnown;
    private final List<ClassXref> references = new ArrayList<>();
    private final ClassTarget classTarget;

    public ClassInput(Execution execution, ClassNode classNode, ClassTarget classTarget) {
        this.execution = execution;
        this.classNode = classNode;
        this.classTarget = classTarget;
    }

    public ClassTarget getClassTarget() {
        return classTarget;
    }

    /**
     * Gives a key that can be used to query {@link ClassInput#methodList}
     */
    private String getMethodKey(String name, String desc) {
        return name + desc;
    }

    /**
     * Creates a new {@link MethodInput} for the relevant method from this class.
     * @param name Name of the method.
     * @param desc Descriptor of the method.
     * @return A newly created {@link MethodInput} to describe this method, or the existing instance of {@link MethodInput}, or {@code null} if this class contains no such method.
     */
    public @Nullable MethodInput createMethod(String name, String desc) {
        final String key = getMethodKey(name, desc);
        MethodInput methodInput = methodList.get(key);
        if (methodInput != null) {
            return methodInput;
        }
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                methodList.put(key, (methodInput = new MethodInput(this, method)));
                return methodInput;
            }
        }
        return null;
    }

    public @Nullable FieldInput createField(String name, String desc) {
        final String key = getMethodKey(name, desc);
        FieldInput fieldInput = fieldList.get(key);
        if (fieldInput != null) {
            return fieldInput;
        }
        for (FieldNode field : classNode.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                fieldList.put(key, (fieldInput = new FieldInput(this, field)));
                return fieldInput;
            }
        }
        return null;
    }

    @Override
    public void populatePopup(PopupItemBuilder builder) {
//        builder.menuItem("View Hierarchy", () -> Main.getDisplayManager().addClosableWindow(new ClassHierarchyWindow(this.getOwningClass().getExecution().getTrinity(), this)));
        super.populatePopup(builder);
    }

    public Map<String, MethodInput> getMethodList() {
        return methodList;
    }

    public Map<String, FieldInput> getFieldList() {
        return fieldList;
    }

    public String getSimpleName() {
        return NameUtil.getSimpleName(this.getFullName());
    }

    /**
     * Gives the full class name.
     * @return Returns {@link ClassNode#name}.
     */
    public String getFullName() {
        return classNode.name;
    }

    public String getDisplayFullName() {
        String displayName = this.getClassTarget().getDisplayName();
        return displayName == null ? this.getFullName() : displayName;
    }

    public String getDisplaySimpleName() {
        return NameUtil.getSimpleName(this.getDisplayFullName());
    }

    /**
     * Gives the full super class name.
     * @return Returns {@link ClassNode#superName}.
     */
    public String getSuperName() {
        return classNode.superName;
    }

    public List<String> getInterfaces() {
        return this.interfaces;
    }

    public Execution getExecution() {
        return execution;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    /**
     * Checks if this class can support another method.
     * @return If the {@link ClassInput#classNode} method list size is less than {@code 65535}.
     */
    public boolean mayHaveAnotherMethod() {
        return classNode.methods.size() < 65535;
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        this.classNode.access = accessFlagsMask;
    }

    @Override
    public void notifyModified(ModifyPriority priority) {
        execution.getTrinity().getEventManager().postEvent(new EventClassModified(this, priority));
    }

    @Override
    public int getAccessFlagsMask() {
        return this.classNode.access;
    }

    public boolean isInterface() {
        return (this.getAccessFlagsMask() & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isFinal() {
        return (this.getAccessFlagsMask() & Opcodes.ACC_FINAL) != 0;
    }

    public AccessFlags getAccessFlags() {
        return accessFlags;
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
    public XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return getClassTarget().createXrefBuilder(xrefMap);
    }

    public MethodInput getMethod(String name, String desc) {
        for (MethodInput method : methodList.values()) {
            if (method.getName().equals(name) && method.getDescriptor().equals(desc)) {
                return method;
            }
        }
        return null;
    }

    public Collection<ClassXref> getReferences() {
        return this.references;
    }

    @Override
    public ClassInput getOwningClass() {
        return this;
    }

    @Override
    public RenameHandler getRenameHandler() {
        return getClassTarget().getRenameHandler();
    }
}
