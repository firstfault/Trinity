package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.database.object.DatabaseClassDisplayName;
import me.f1nal.trinity.decompiler.struct.attr.StructInnerClassesAttribute;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.xref.ClassXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.cp.FileKind;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.IXrefBuilderProvider;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderClassRef;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.IDisplayNameProvider;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.remap.RenameType;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.checkerframework.checker.units.qual.C;

import java.util.Collection;

/**
 * Class reference object, even if we don't have it as an input.
 */
public class ClassTarget extends ArchiveEntry implements IDatabaseSavable<DatabaseClassDisplayName>, IXrefBuilderProvider, IDisplayNameProvider {
    private ClassInput input;
    private String realName;
    private final DisplayName displayName;

    public ClassTarget(String realName, int size) {
        super(size);
        this.realName = realName;
        this.displayName = new DisplayName(this.realName);
    }

    @Override
    public RenameHandler getRenameHandler() {
        return new RenameHandler() {
            @Override
            public void rename(Remapper remapper, String newName) {
                if (getInput() != null) {
                    remapper.renameClass(ClassTarget.this, newName);
                }
            }

            @Override
            public String getFullName() {
                return getDisplayOrRealName();
            }
        };
    }

    /**
     * Simply put, tells us if our class path contains this class.
     * @return If we have a {@link ClassInput} linked to this reference.
     */
    public boolean isInputAvailable() {
        return this.getInput() != null;
    }

    @Override
    public void setName(String newName) {
        this.getDisplayName().setName(newName);
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
        return DataPool.writeClassNode(this.getInput().getNode());
    }

    public void setInput(ClassInput input) {
        this.input = input;
    }

    public String getRealName() {
        return realName;
    }

    @Override
    public String getDisplayOrRealName() {
        return displayName.getName();
    }

    public ClassInput getInput() {
        return input;
    }

    @Override
    public DisplayName getDisplayName() {
        return displayName;
    }

    @Override
    public String getArchiveEntryTypeName() {
        return this.getKind().getFileType();
    }

    @Override
    public DatabaseClassDisplayName createDatabaseObject() {
        return new DatabaseClassDisplayName(this.getRealName(), this.getDisplayName().getName());
    }

    private FileKind kind;

    @Override
    public FileKind getKind() {
        if (this.kind == null) {
            this.kind = this.findKind();
        }
        return this.kind;
    }

    private FileKind findKind() {
        if (getInput() != null) {
            final AccessFlags accessFlags = getInput().getAccessFlags();
            if (accessFlags.isAnnotation()) {
                return FileKind.ANNOTATION;
            }
            if (accessFlags.isInterface()) {
                return FileKind.INTERFACES;
            }
            if (accessFlags.isEnum()) {
                return FileKind.ENUM;
            }
            if (accessFlags.isAbstract()) {
                return FileKind.ABSTRACT;
            }
        } else {
            Trinity trinity = Main.getTrinity();
            if (trinity != null) {
                Collection<ClassXref> references = trinity.getExecution().getXrefMap().getReferences(this);

                for (ClassXref reference : references) {
                    // Is this a good way...?
                    if (reference.getKind() == XrefKind.INHERIT && reference.getInvocation().equals("Implements")) {
                        return FileKind.INTERFACES;
                    }
                }
            }
        }
        return FileKind.CLASSES;
    }

    public void resetKind() {
        kind = null;
    }

    @Override
    public XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return new XrefBuilderClassRef(xrefMap, this.getRealName());
    }
}
