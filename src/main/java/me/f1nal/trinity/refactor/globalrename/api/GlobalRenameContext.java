package me.f1nal.trinity.refactor.globalrename.api;

import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.remap.NameHeuristics;

import java.util.List;

public class GlobalRenameContext {
    private final Execution execution;
    private final List<Rename> renames;
    private final NameHeuristics nameHeuristics;

    public GlobalRenameContext(Execution execution, List<Rename> renames, NameHeuristics nameHeuristics) {
        this.execution = execution;
        this.renames = renames;
        this.nameHeuristics = nameHeuristics;
    }

    public Execution execution() {
        return execution;
    }

    public List<Rename> renames() {
        return renames;
    }

    public NameHeuristics nameHeuristics() {
        return nameHeuristics;
    }
}
