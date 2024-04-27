package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseFieldDisplayName;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.remap.Remapper;
import org.objectweb.asm.tree.FieldNode;

public class FieldInput extends MemberInput<FieldNode> implements IDatabaseSavable<DatabaseFieldDisplayName> {
    public FieldInput(FieldNode node, ClassInput owner) {
        super(node, owner, new MemberDetails(owner.getFullName(), node.name, node.desc));
    }

    @Override
    public InputType getType() {
        return InputType.FIELD;
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        getNode().access = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return getNode().access;
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
    public DatabaseFieldDisplayName createDatabaseObject() {
        return new DatabaseFieldDisplayName(this.getDetails(), this.getDisplayName());
    }
}
