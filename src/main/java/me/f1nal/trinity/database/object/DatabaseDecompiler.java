package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;

import java.util.Objects;

public class DatabaseDecompiler extends AbstractDatabaseObject {
    private static final byte FLAG_ENUM_PRESENTATION_SET = 1;
    private static final byte FLAG_ENUM_AS_CLASS = 1 << 1;

    private final String className;
    /**
     * Window-specific decompiler settings. A separate "set" bit lets databases created before
     * this field existed continue to use the current global preference as their default.
     */
    private final byte flags;

    public DatabaseDecompiler(String className) {
        this(className, (byte) 0);
    }

    public DatabaseDecompiler(String className, byte flags) {
        this.className = className;
        this.flags = flags;
    }

    public static byte createFlags(boolean treatEnumAsClass) {
        return (byte) (FLAG_ENUM_PRESENTATION_SET
                | (treatEnumAsClass ? FLAG_ENUM_AS_CLASS : 0));
    }

    public boolean hasEnumPresentation() {
        return (this.flags & FLAG_ENUM_PRESENTATION_SET) != 0;
    }

    public boolean isEnumAsClass() {
        return (this.flags & FLAG_ENUM_AS_CLASS) != 0;
    }

    @Override
    public boolean load(Trinity trinity) {
        ClassInput classInput = trinity.getExecution().getClassInput(this.className);
        if (classInput == null) {
            return false;
        }
        DecompilerWindow window = Main.getDisplayManager().openDecompilerView(classInput);
        if (this.hasEnumPresentation()) {
            window.restoreEnumPresentation(this.isEnumAsClass());
        }
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("decompilerObj");
    }
}
