package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.windows.impl.cp.FileKind;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.NameUtil;

public class ResourceArchiveEntry extends ArchiveEntry {
    private final String type;
    private String name;
    private final byte[] bytes;
    
    public ResourceArchiveEntry(String name, byte[] bytes) {
        super(bytes.length);
        this.name = name;
        final String extension = NameUtil.getExtension(name);
        this.type = extension == null ? "Binary File" : extension;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public RenameHandler getRenameHandler() {
        return (newName) -> Main.getTrinity().getExecution().renameResource(this, newName);
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    protected int getIconColor() {
        return CodeColorScheme.TEXT;
    }

    @Override
    protected String getIcon() {
        return FontAwesomeIcons.File;
    }

    @Override
    public byte[] extract() {
        return this.bytes;
    }

    @Override
    public String getRealName() {
        return this.name;
    }

    @Override
    public String getDisplayOrRealName() {
        return this.name;
    }

    @Override
    public String getArchiveEntryTypeName() {
        return this.type;
    }

    @Override
    public IKindType getKind() {
        return FileKind.RESOURCE;
    }
}
