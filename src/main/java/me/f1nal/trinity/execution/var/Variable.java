package me.f1nal.trinity.execution.var;

import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.database.object.DatabaseVariable;
import imgui.type.ImString;

public class Variable implements IDatabaseSavable {
    private final VariableTable table;
    private ImString nameProperty;
    private String name;

    public Variable(VariableTable table, String name) {
        this.table = table;
        this.name = name;
    }

    public VariableTable getTable() {
        return table;
    }

    public String getName() {
        return nameProperty != null ? nameProperty.get() : name;
    }

    public ImString getNameProperty() {
        if (this.nameProperty == null) {
            this.nameProperty = new ImString(name, 0x100);
        }
        return this.nameProperty;
    }

    public boolean isEditable() {
        return true;
    }

    public int findIndex() {
        return table.getIndex(this);
    }

    @Override
    public AbstractDatabaseObject createDatabaseObject() {
        return new DatabaseVariable(table.getMethod().getDetails(), findIndex(), this.getName());
    }
}
