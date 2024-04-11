package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.logging.Logging;

import java.util.Objects;

public class DatabaseClassDisplayName extends AbstractDatabaseObject {
    private final String className;
    private final String displayName;

    public DatabaseClassDisplayName(String className, String displayName) {
        this.className = className;
        this.displayName = displayName;
    }

    public String getClassName() {
        return className;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean load(Trinity trinity) {
        ClassTarget target = trinity.getExecution().getClassTarget(this.className);
        if (target == null) {
            Logging.warn("Class {} can no longer be found", this.className);
            return false;
        }
        target.getDisplayName().setName(this.getDisplayName());
        target.setPackage(trinity.getExecution().getRootPackage());
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("classDisplayName", this.className);
    }
}
