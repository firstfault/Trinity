package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;

import java.util.Objects;

public class DatabaseDecompiler extends AbstractDatabaseObject {
    private final String className;

    public DatabaseDecompiler(String className) {
        this.className = className;
    }

    @Override
    public boolean load(Trinity trinity) {
        ClassInput classInput = trinity.getExecution().getClassInput(this.className);
        if (classInput == null) {
            return false;
        }
        Main.getDisplayManager().openDecompilerView(classInput);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("decompilerObj");
    }
}
