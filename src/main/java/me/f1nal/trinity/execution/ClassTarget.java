package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.database.object.DatabaseClassDisplayName;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.frames.impl.cp.FileKind;
import me.f1nal.trinity.gui.frames.impl.cp.RenameHandler;
import me.f1nal.trinity.theme.CodeColorScheme;

/**
 * Class reference object, even if we don't have it as an input.
 */
public class ClassTarget extends ArchiveEntry implements IDatabaseSavable<DatabaseClassDisplayName> {
    private ClassInput input;
    private String realName;
    private String displayName;

    public ClassTarget(String realName, int size) {
        super(size);
        this.realName = realName;
    }

    @Override
    protected RenameHandler getRenameHandler() {
        return new RenameHandler() {
            @Override
            public void rename(String newName) {
                if (getInput() != null) {
                    Main.getTrinity().getRemapper().renameClass(ClassTarget.this, newName);
                }
            }

            @Override
            public String getFullName() {
                return getDisplayOrRealName();
            }
        };
    }

    @Override
    public void setName(String newName) {
        this.setDisplayName(newName);
    }

    @Override
    protected int getIconColor() {
        return CodeColorScheme.CLASS_REF;
    }

    @Override
    protected String getIcon() {
        return FontAwesomeIcons.FileCode;
    }

    @Override
    public byte[] extract() {
        if (this.getInput() == null) {
            return null;
        }
        return DataPool.writeClassNode(this.getInput().getClassNode());
    }

    public void setInput(ClassInput input) {
        this.input = input;
    }

    public String getRealName() {
        return realName;
    }

    public ClassInput getInput() {
        return input;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayOrRealName() {
        return displayName == null ? realName : displayName;
    }

    @Override
    public String getArchiveEntryTypeName() {
        return this.getKind().getFileType();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public DatabaseClassDisplayName createDatabaseObject() {
        return new DatabaseClassDisplayName(this.getRealName(), this.getDisplayName());
    }

    @Override
    public FileKind getKind() {
        if (getInput() != null) {
            final AccessFlags accessFlags = getInput().getAccessFlags();
            if (accessFlags.isInterface()) {
                return FileKind.INTERFACES;
            }
            if (accessFlags.isAnnotation()) {
                return FileKind.ANNOTATION;
            }
            if (accessFlags.isEnum()) {
                return FileKind.ENUM;
            }
            if (accessFlags.isAbstract()) {
                return FileKind.ABSTRACT;
            }
        }
        return FileKind.CLASSES;
    }
}
