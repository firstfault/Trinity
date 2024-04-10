package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseMethodDisplayName;
import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.var.VariableTable;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerFrame;
import me.f1nal.trinity.gui.windows.impl.assembler.line.Instruction2SourceMapping;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MethodInput extends Input implements IDatabaseSavable<DatabaseMethodDisplayName> {
    private final ClassInput owningClass;
    private final MethodNode methodNode;
    private final AccessFlags accessFlags;
    private final VariableTable variableTable;
    private final LabelTable labelTable = new LabelTable();
    private String displayName;
    private XrefWhere xrefWhere;

    public MethodInput(ClassInput owningClass, MethodNode methodNode) {
        this.owningClass = owningClass;
        this.methodNode = methodNode;
        this.accessFlags = new AccessFlags(this.getOwningClass(), this);
        this.setDisplayName(this.getName());
        this.variableTable = new VariableTable(this);
    }

    public XrefWhere getXrefWhere() {
        if (xrefWhere == null) {
            xrefWhere = new XrefWhereMethod(this);
        }
        return xrefWhere;
    }

    public RenameHandler getRenameHandler() {
        return isInit() ? getOwningClass().getRenameHandler() : new RenameHandler() {
            @Override
            public String getFullName() {
                return getDisplayName();
            }

            @Override
            public void rename(String newName) {
                MethodInput.this.rename(Main.getTrinity().getRemapper(), newName);
            }
        };
    }

    public void setDisplayName(String displayName) {
        this.displayName = NameUtil.cleanNewlines(displayName);
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public LabelTable getLabelTable() {
        return labelTable;
    }

    public MemberDetails getDetails() {
        return new MemberDetails(this.getOwningClass().getFullName(), this.getName(), this.getDescriptor());
    }

    // This is used for AbstractInsnNode#clone(Map)
    public Map<LabelNode, LabelNode> createLabelMap() {
        final Map<LabelNode, LabelNode> map = new HashMap<>();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if(instruction instanceof LabelNode labelNode) map.put(labelNode, labelNode);
        }

        return map;
    }

    // TODO: :p
    public boolean isInvokeDynamicCallsite() {
        return getName().contains("lambda$");
    }

    public InsnList getInstructions() {
        return methodNode.instructions;
    }

    public ClassInput getOwningClass() {
        return owningClass;
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public AccessFlags getAccessFlags() {
        return accessFlags;
    }

    @Override
    public boolean isAccessFlagValid(AccessFlags.Flag flag) {
        return flag.isMethodFlag();
    }

    @Override
    public void rename(Remapper remapper, String newName) {
        remapper.renameMethod(this, newName);
    }

    @Override
    public void populatePopup(PopupItemBuilder builder) {
        super.populatePopup(builder);

        builder.menuItem("Assemble", () -> {
            Main.getWindowManager().addClosableWindow(new AssemblerFrame(Main.getTrinity(), this, new Instruction2SourceMapping()));
        });
    }

    @Override
    public XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return new XrefBuilderMemberRef(xrefMap, this.getDetails());
    }

    public String getName() {
        return methodNode.name;
    }

    public String getDescriptor() {
        return methodNode.desc;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getFullDisplayName() {
        return owningClass.getDisplayFullName() + "." + this.getDisplayName() + "#" + methodNode.desc;
    }

    public boolean isInitOrClinit() {
        return isInit() || isClinit();
    }

    public boolean isClinit() {
        return getName().equals("<clinit>") && getDescriptor().equals("()V");
    }

    public boolean isInit() {
        return getName().equals("<init>");
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        this.methodNode.access = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return this.methodNode.access;
    }

    public List<String> getExceptions() {
        return methodNode.exceptions;
    }

    public String getNameWithDesc() {
        return getName() + getDescriptor();
    }

    @Override
    public DatabaseMethodDisplayName createDatabaseObject() {
        return new DatabaseMethodDisplayName(this.getDetails(), this.getDisplayName());
    }

    @Override
    public String toString() {
        return getDetails().getAll();
    }
}
