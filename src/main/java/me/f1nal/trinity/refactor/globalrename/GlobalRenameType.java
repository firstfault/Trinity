package me.f1nal.trinity.refactor.globalrename;

import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;

import java.util.List;

public abstract class GlobalRenameType implements INameable, IDescribable {
    private final String name;
    private final String tooltipDescription;

    protected GlobalRenameType(String name, String tooltipDescription) {
        this.name = name;
        this.tooltipDescription = tooltipDescription;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return tooltipDescription;
    }

    /**
     * Draw user inputs required for this global rename.
     */
    public abstract void drawInputs();
    public abstract void runRefactor(Execution execution, List<Rename> renames);
}
