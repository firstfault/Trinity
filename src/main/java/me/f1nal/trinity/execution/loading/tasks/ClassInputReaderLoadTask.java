package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.inputs.ProjectContainerInput;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

/** Parses and installs all project inputs while retaining their container ownership. */
public class ClassInputReaderLoadTask extends ProgressiveLoadTask implements ICaption {
    private final ProjectInputSet projectInput;
    private final boolean activeImport;
    private final BooleanSupplier installationAllowed;
    private volatile int installedContainerCount;
    private volatile boolean installationCancelled;

    public ClassInputReaderLoadTask(ProjectInputSet projectInput) {
        this(projectInput, false, () -> true);
    }

    private ClassInputReaderLoadTask(ProjectInputSet projectInput, boolean activeImport,
                                     BooleanSupplier installationAllowed) {
        super("Reading Input");
        this.projectInput = projectInput == null ? new ProjectInputSet() : projectInput;
        this.activeImport = activeImport;
        this.installationAllowed = installationAllowed;
    }

    public static ClassInputReaderLoadTask forActiveImport(ProjectInputSet projectInput,
                                                            BooleanSupplier installationAllowed) {
        return new ClassInputReaderLoadTask(projectInput, true, installationAllowed);
    }

    @Override
    public void runImpl() {
        int classCount = projectInput.getContainers().stream()
                .mapToInt(input -> input.getClassPath().getClasses().size()).sum();
        this.startWork(Math.max(1, classCount));

        Execution execution = getTrinity().getExecution();
        Set<String> reservedNames = new HashSet<>();
        List<ParsedContainer> parsedContainers = new ArrayList<>();
        List<RejectedContainer> rejectedContainers = new ArrayList<>();

        for (ProjectContainerInput input : projectInput.getContainers()) {
            ParsedContainer parsed = parseContainer(input, execution, reservedNames, rejectedContainers);
            if (parsed != null) parsedContainers.add(parsed);
        }
        if (classCount == 0) this.finishedWork();

        var installation = Main.runLater(() ->
                installTransaction(execution, parsedContainers, rejectedContainers));
        try {
            installation.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        }
    }

    private ParsedContainer parseContainer(ProjectContainerInput input, Execution execution, Set<String> reservedNames,
                                           List<RejectedContainer> rejectedContainers) {
        List<ClassTarget> targets = new ArrayList<>();
        Set<String> localNames = new HashSet<>();
        String failure = null;

        for (UnreadClassBytes unread : input.getClassPath().getClasses()) {
            ClassNode classNode = readClassNode(unread.getBytes());
            if (classNode == null) {
                failure = "could not parse " + unread.getEntryName();
                finishedWork();
                continue;
            }
            if (!localNames.add(classNode.name) || reservedNames.contains(classNode.name)) {
                failure = "duplicate class " + classNode.name;
                finishedWork();
                continue;
            }

            ClassTarget target = createClassTarget(execution, classNode, unread);
            targets.add(target);
            finishedWork();
        }

        if (failure != null) {
            rejectedContainers.add(new RejectedContainer(input.getName(), failure));
            return null;
        }
        reservedNames.addAll(localNames);
        return new ParsedContainer(input, targets);
    }

    private void installTransaction(Execution execution, List<ParsedContainer> parsedContainers,
                                    List<RejectedContainer> rejectedContainers) {
        if (!installationAllowed.getAsBoolean()) {
            installationCancelled = true;
            return;
        }

        rejectedContainers.forEach(rejected -> notifyRejected(rejected.container(), rejected.reason()));
        Set<String> reservedNames = getInstalledClassNames(execution);
        List<ParsedContainer> acceptedContainers = new ArrayList<>();
        for (ParsedContainer parsed : parsedContainers) {
            String duplicate = reserveClassNames(reservedNames, parsed.targets());
            if (duplicate == null) {
                acceptedContainers.add(parsed);
            } else {
                notifyRejected(parsed.input().getName(), "duplicate class " + duplicate);
            }
        }

        for (ParsedContainer parsed : acceptedContainers) installContainer(execution, parsed);
        installedContainerCount = acceptedContainers.size();
        if (activeImport && installedContainerCount != 0) {
            execution.refreshStructuralIndexes();
            getTrinity().getEventManager().postEvent(new EventClassesLoaded());
        }
    }

    private Set<String> getInstalledClassNames(Execution execution) {
        Set<String> names = new HashSet<>();
        execution.getClassList().forEach(input -> names.add(input.getClassTarget().getRealName()));
        execution.getClassTargetMap().values().stream()
                .filter(target -> target.getInput() != null)
                .forEach(target -> names.add(target.getRealName()));
        return names;
    }

    static String reserveClassNames(Set<String> reservedNames, List<ClassTarget> targets) {
        Set<String> candidateNames = new HashSet<>();
        for (ClassTarget target : targets) {
            String name = target.getRealName();
            if (reservedNames.contains(name) || !candidateNames.add(name)) return name;
        }
        reservedNames.addAll(candidateNames);
        return null;
    }

    private ClassTarget createClassTarget(Execution execution, ClassNode classNode, UnreadClassBytes unread) {
        ClassTarget classTarget = new ClassTarget(classNode.name, unread.getBytes().length, unread.getMetadata());
        ClassInput classInput = new ClassInput(execution, classNode, classTarget, unread.getBytes(),
                unread.getEntryName(), unread.isRebuildRequired());
        classTarget.setInput(classInput);
        classNode.methods.forEach(method -> classInput.addInput(new MethodInput(method, classInput)));
        classNode.fields.forEach(field -> classInput.addInput(new FieldInput(field, classInput)));
        return classTarget;
    }

    private void installContainer(Execution execution, ParsedContainer parsed) {
        ProjectContainerInput input = parsed.input();
        ProjectContainer container = input.getKind() == ProjectContainerKind.LOOSE
                ? execution.getContainers().stream()
                    .filter(existing -> existing.getKind() == ProjectContainerKind.LOOSE)
                    .findFirst().orElse(null)
                : null;
        if (container == null) {
            container = new ProjectContainer(input.getId(), input.getName(), input.getKind(),
                    getTrinity().getDatabase());
            container.setArchiveComment(input.getClassPath().getArchiveComment());
            ProjectContainer newContainer = container;
            input.getClassPath().getDirectories().forEach(directory -> newContainer.addDirectory(
                    new me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry(
                            directory.getName(), directory.getMetadata().copy())));
            execution.addContainer(container);
        }

        for (ClassTarget target : parsed.targets()) {
            execution.addClassTarget(target);
            target.setPackage(container.getRootPackage(), false);
            execution.getClassList().add(target.getInput());
        }
        ProjectContainer installedContainer = container;
        input.getClassPath().getResources().forEach((name, bytes) ->
                new ResourceArchiveEntry(name, bytes,
                        input.getClassPath().getResourceMetadata(name)).setPackage(installedContainer.getRootPackage(), false));
    }

    private void notifyRejected(String container, String reason) {
        Main.getDisplayManager().addNotification(new Notification(NotificationType.ERROR, this,
                ColoredStringBuilder.create().fmt("Could not import {}: {}", container, reason).get()));
    }

    private ClassNode readClassNode(byte[] bytes) {
        ClassNode classNode = readClassNode(bytes, 0);
        return classNode == null ? readClassNode(bytes, ClassReader.SKIP_DEBUG) : classNode;
    }

    private ClassNode readClassNode(byte[] bytes, int flags) {
        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(bytes).accept(classNode, flags);
            return classNode;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public String getCaption() {
        return getName();
    }

    public int getInstalledContainerCount() {
        return installedContainerCount;
    }

    public boolean isInstallationCancelled() {
        return installationCancelled;
    }

    private record ParsedContainer(ProjectContainerInput input, List<ClassTarget> targets) {
    }

    private record RejectedContainer(String container, String reason) {
    }
}
