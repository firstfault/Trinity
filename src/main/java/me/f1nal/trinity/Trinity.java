package me.f1nal.trinity;

import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.decompiler.Decompiler;
import me.f1nal.trinity.events.api.EventManager;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.constant.ConstantStatisticsCache;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.execution.xref.XrefViewerSettings;
import me.f1nal.trinity.refactor.RefactorManager;
import me.f1nal.trinity.remap.Remapper;

import java.io.File;
import java.io.IOException;

public final class Trinity {
    /**
     * Program's execution flow, managing the sequence of operations.
     */
    private final Execution execution;
    /**
     * Remapping of members (fields, methods, classes) of this Trinity database.
     */
    private final Remapper remapper;
    /**
     * Database this Trinity instance is operating on.
     * @see Database
     */
    private final Database database;
    /**
     * Decompiler-related behavior.
     */
    private final Decompiler decompiler;
    /**
     * Event manager bound to this Trinity instance.
     */
    private final EventManager eventManager;
    /**
     * Cached project-wide string and number occurrence statistics.
     */
    private final ConstantStatisticsCache constantStatisticsCache;
    /**
     * Project-level xref viewer filter preferences.
     */
    private final XrefViewerSettings xrefViewerSettings;
    /**
     * Automatic refactoring.
     */
    private final RefactorManager refactorManager;

    public Trinity(Database database, ProjectInputSet projectInput) throws IOException, MissingEntryPointException {
        this.database = database;
        this.execution = new Execution(this, projectInput);
        this.remapper = new Remapper(this.execution);
        this.eventManager = new EventManager();
        this.constantStatisticsCache = this.eventManager.registerListener(new ConstantStatisticsCache(this.execution));
        this.xrefViewerSettings = new XrefViewerSettings();
        this.refactorManager = new RefactorManager(this);
        this.decompiler = this.eventManager.registerListener(new Decompiler(this));
        this.execution.getAsynchronousLoad().execute();
    }

    public Execution getExecution() {
        return execution;
    }

    public Remapper getRemapper() {
        return remapper;
    }

    public Database getDatabase() {
        return database;
    }

    public Decompiler getDecompiler() {
        return decompiler;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ConstantStatisticsCache getConstantStatisticsCache() {
        return constantStatisticsCache;
    }

    public XrefViewerSettings getXrefViewerSettings() {
        return xrefViewerSettings;
    }

    public RefactorManager getRefactorManager() {
        return refactorManager;
    }
}
