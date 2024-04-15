package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseMethodDisplayName;
import me.f1nal.trinity.execution.hierarchy.MethodHierarchy;
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
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MethodInput extends MemberInput<MethodNode> implements IDatabaseSavable<DatabaseMethodDisplayName> {
    private final VariableTable variableTable;
    private final LabelTable labelTable = new LabelTable(this);
    private MethodHierarchy methodHierarchy;

    public MethodInput(MethodNode node, ClassInput owner) {
        super(node, owner, new MemberDetails(owner.getFullName(), node.name, node.desc));
        this.variableTable = new VariableTable(this);
    }

    public void setMethodHierarchy(MethodHierarchy methodHierarchy) {
        this.methodHierarchy = methodHierarchy;
    }

    public MethodHierarchy getMethodHierarchy() {
        return methodHierarchy;
    }

    public RenameHandler getRenameHandler() {
        return isInit() ? getOwningClass().getRenameHandler() : new RenameHandler() {
            @Override
            public String getFullName() {
                return getDisplayName().getName();
            }

            @Override
            public void rename(Remapper remapper, String newName) {
                MethodInput.this.rename(remapper, newName);
            }
        };
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public LabelTable getLabelTable() {
        return labelTable;
    }

    // This is used for AbstractInsnNode#clone(Map)
    public Map<LabelNode, LabelNode> createLabelMap() {
        final Map<LabelNode, LabelNode> map = new HashMap<>();
        for (AbstractInsnNode instruction : getNode().instructions) {
            if(instruction instanceof LabelNode labelNode) map.put(labelNode, labelNode);
        }

        return map;
    }

    public InsnList getInstructions() {
        return getNode().instructions;
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
        return isClinit() ? getOwningClass().createXrefBuilder(xrefMap) : super.createXrefBuilder(xrefMap);
    }

    public String getName() {
        return getNode().name;
    }

    @Override
    public InputType getType() {
        return InputType.METHOD;
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
        this.getNode().access = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return this.getNode().access;
    }

    @Override
    public DatabaseMethodDisplayName createDatabaseObject() {
        return new DatabaseMethodDisplayName(this.getDetails(), this.getDisplayName());
    }
}
