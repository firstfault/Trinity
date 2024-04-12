package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.RenameType;

import java.util.Objects;

public class DatabaseMethodDisplayName extends AbstractDatabaseObject {
    private final MemberDetails details;
    private final String displayName;
    private final RenameType renameType;

    public DatabaseMethodDisplayName(MemberDetails details, DisplayName displayName) {
        this.details = details;
        this.displayName = displayName.getName();
        this.renameType = displayName.getType();
    }

    @Override
    public boolean load(Trinity trinity) {
        final MethodInput methodInput = trinity.getExecution().getMethod(this.details);
        if (methodInput == null) {
            Logging.warn("Database has no method correlating to {}.", this.details);
            return false;
        }
        methodInput.getDisplayName().setName(displayName, this.renameType);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("methodDisplayName", this.details);
    }
}
