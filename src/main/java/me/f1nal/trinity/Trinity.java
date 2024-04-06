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
     * Represents the program's execution flow, managing the sequence of operations.
     * This field encapsulates the control of program execution.
     */
    private final Execution execution;
    private final Remapper remapper;
    /**
     * Represents input from the Java Runtime class.
     */
    private final JrtInput jrtInput = new JrtInput();
    private final Database database;
    private final Decompiler decompiler;
    private final EventManager eventManager;
    private final RefactorManager refactorManager;

    /**
     * Constructs a new Trinity instance for the given JAR file.
     * <p>
     * This constructor initializes a Trinity instance with the provided JAR input stream
     * and file name. It aims to encapsulate the behavior of processing the JAR file to
     * identify the entry point and manage related operations.
     * </p>
     *
     * @throws IOException If an I/O error occurs while processing the JAR input stream.
     * @throws MissingEntryPointException If the entry point cannot be located in the JAR file.
     */
    public Trinity(Database database, ClassPath classPath) throws IOException, MissingEntryPointException {
        this.database = database;
        this.execution = new Execution(this, classPath);
        this.remapper = new Remapper(this.execution);
        this.eventManager = new EventManager();
        this.refactorManager = new RefactorManager(this);
        this.decompiler = this.eventManager.registerListener(new Decompiler(this));
        this.execution.getAsynchronousLoad().execute();
    }

    public void runDeobf() {
//        try {
//            new RavenStringDeobf(this).runPass();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        new RavenNumberDeobf(this).runPass();
//        new RavenString2Deobf(this).runPass();
//        new RavenFlow1Deobf(this).runPass();
    }

    /**
     * Returns the execution control flow instance.
     * @return The execution instance.
     */
    public Execution getExecution() {
        return execution;
    }

    /**
     * Returns the remapper instance for name remapping.
     * @return The remapper instance.
     */
    public Remapper getRemapper() {
        return remapper;
    }

    /**
     * Returns the input manager for Java Runtime classes.
     * @return The JRT input manager.
     */
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
