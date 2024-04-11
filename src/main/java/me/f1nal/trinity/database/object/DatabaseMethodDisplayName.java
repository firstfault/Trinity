package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.logging.Logging;

import java.util.Objects;

public class DatabaseMethodDisplayName extends AbstractDatabaseObject {
    private final MemberDetails details;
    private final String displayName;

    public DatabaseMethodDisplayName(MemberDetails details, String displayName) {
        this.details = details;
        this.displayName = displayName;
    }

    @Override
    public boolean load(Trinity trinity) {
        final MethodInput methodInput = trinity.getExecution().getMethod(this.details);
        if (methodInput == null) {
            Logging.warn("Database has no method correlating to {}.", this.details);
            return false;
        }
        methodInput.getDisplayName().setName(displayName);
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("methodDisplayName", this.details);
    }
}
