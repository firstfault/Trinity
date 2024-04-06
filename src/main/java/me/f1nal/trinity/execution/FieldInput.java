package me.f1nal.trinity.execution;

import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseFieldDisplayName;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.ModifyNotifiable;
import me.f1nal.trinity.util.ModifyPriority;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.tree.FieldNode;

public class FieldInput extends Input implements IDatabaseSavable<DatabaseFieldDisplayName>, ModifyNotifiable {
    private final ClassInput classInput;
    private final FieldNode fieldNode;
    private String displayName;
    private final AccessFlags accessFlags = new AccessFlags(this, this);

    public FieldInput(ClassInput classInput, FieldNode fieldNode) {
        this.classInput = classInput;
        this.fieldNode = fieldNode;
        this.setDisplayName(this.getName());
    }

    public String getDisplayName() {
        return displayName;
    }

    private String getName() {
        return fieldNode.name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = NameUtil.cleanNewlines(displayName);
    }

    public FieldNode getFieldNode() {
        return fieldNode;
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        this.fieldNode.access = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return this.fieldNode.access;
    }

    public AccessFlags getAccessFlags() {
        return accessFlags;
    }

    @Override
    public boolean isAccessFlagValid(AccessFlags.Flag flag) {
        return flag.isFieldFlag();
    }

    @Override
    public void rename(Remapper remapper, String newName) {
        remapper.renameField(this, newName);
    }

    @Override
    protected XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return new XrefBuilderMemberRef(xrefMap, this.getDetails());
    }

    public String getFullDisplayName() {
        return this.classInput.getDisplayFullName() + "." + this.getDisplayName() + "#" + this.fieldNode.desc;
    }

    public String getRealName() {
        return this.fieldNode.name;
    }

    @Override
    public ClassInput getOwningClass() {
        return this.classInput;
    }

    @Override
    public DatabaseFieldDisplayName createDatabaseObject() {
        return new DatabaseFieldDisplayName(this.getDetails(), this.getDisplayName());
    }

    public MemberDetails getDetails() {
        return new MemberDetails(this.getOwningClass().getFullName(), this.fieldNode.name, this.fieldNode.desc);
    }

    @Override
    public void notifyModified(ModifyPriority priority) {

    }

    public String getDescriptor() {
        return fieldNode.desc;
    }
}
