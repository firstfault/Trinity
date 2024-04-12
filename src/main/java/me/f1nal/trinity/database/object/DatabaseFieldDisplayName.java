package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.RenameType;

import java.util.Objects;

public class DatabaseFieldDisplayName extends AbstractDatabaseObject {
    private final MemberDetails details;
    private final String displayName;
    private final RenameType renameType;

    public DatabaseFieldDisplayName(MemberDetails details, DisplayName displayName) {
        this.details = details;
        this.displayName = displayName.getName();
        this.renameType = displayName.getType();
    }

    public MemberDetails getDetails() {
        return details;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RenameType getRenameType() {
        return renameType;
    }

    @Override
    public boolean load(Trinity trinity) {
        final FieldInput methodInput = trinity.getExecution().getField(this.details);
        if (methodInput == null) {
            Logging.warn("Database has no field correlating to {}.", this.details);
            return false;
        }
        methodInput.getDisplayName().setName(displayName, this.getRenameType());
        return true;
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("fieldDisplayName", this.details);
    }
}
