package me.f1nal.trinity;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.decompiler.Decompiler;
import me.f1nal.trinity.events.api.EventManager;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.input.JrtInput;
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
     * Input from the current Java runtime.
     */
    private static final JrtInput jrtInput = new JrtInput();
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
     * Automatic refactoring.
     */
    private final RefactorManager refactorManager;

    public Trinity(Database database, ClassPath classPath) throws IOException, MissingEntryPointException {
        this.database = database;
        this.execution = new Execution(this, classPath);
        this.remapper = new Remapper(this.execution);
        this.eventManager = new EventManager();
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

    public JrtInput getJrtInput() {
        return jrtInput;
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

    public RefactorManager getRefactorManager() {
        return refactorManager;
    }
}
