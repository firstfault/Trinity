package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.logging.Logging;

import java.util.Objects;

public class DatabaseVariable extends AbstractDatabaseObject {
    private final MemberDetails method;
    private final int index;
    private final String name;

    public DatabaseVariable(MemberDetails method, int index, String name) {
        this.method = method;
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean load(Trinity trinity) {
        final MethodInput methodInput = trinity.getExecution().getMethod(method);
        if (methodInput == null) {
            Logging.warn("Database has no method correlating to variable {}.", this.getDebugInformation());
            return false;
        }
        Variable variable = methodInput.getVariableTable().getVariable(this.index);
        if (variable == null) {
            Logging.warn("Database has an invalid variable {}.", this.getDebugInformation());
            return false;
        }
        if (!variable.isEditable()) {
            Logging.warn("Database tried to update variable {} that is not editable.", this.getDebugInformation());
            return false;
        }
        variable.getNameProperty().set(this.name);
        return true;
    }

    private String getDebugInformation() {
        return this.method.getKey() + "#" + index + "(" + name + ")";
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("variable", method, index);
    }
}
