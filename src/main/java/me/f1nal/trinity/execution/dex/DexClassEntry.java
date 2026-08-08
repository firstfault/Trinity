package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.windows.impl.cp.FileKind;
import me.f1nal.trinity.gui.windows.impl.cp.RenameHandler;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** First-class project-browser entry for one class defined in a DEX file. */
public final class DexClassEntry extends ArchiveEntry {
    private final DexFileUnit file;
    private final ClassDef classDef;
    private final String internalName;

    DexClassEntry(DexFileUnit file, ClassDef classDef) {
        super(0);
        this.file = Objects.requireNonNull(file, "file");
        this.classDef = Objects.requireNonNull(classDef, "classDef");
        this.internalName = DexDescriptors.internalName(classDef.getType());
    }

    public DexFileUnit getFile() {
        return file;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public String getInternalName() {
        return internalName;
    }

    public Method findMethod(String name, String descriptor) {
        for (Method method : classDef.getMethods()) {
            if (method.getName().equals(name)
                    && DexDescriptors.methodDescriptor(method).equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    public String disassemble() {
        return DexDisassembler.disassembleClass(file, classDef);
    }

    @Override
    public RenameHandler getRenameHandler() {
        return null;
    }

    @Override
    public void setName(String newName) {
        throw new UnsupportedOperationException("DEX classes are read-only");
    }

    @Override
    protected int getIconColor() {
        return CodeColorScheme.FILE_CLASS;
    }

    @Override
    protected String getIcon() {
        return CodiconIcons.SYMBOL_CLASS;
    }

    @Override
    public byte[] extract() {
        return disassemble().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getRealName() {
        return internalName;
    }

    @Override
    public String getDisplayOrRealName() {
        return internalName;
    }

    @Override
    public String getArchiveEntryTypeName() {
        return "DEX Class";
    }

    @Override
    public IKindType getKind() {
        return FileKind.DEX;
    }
}
