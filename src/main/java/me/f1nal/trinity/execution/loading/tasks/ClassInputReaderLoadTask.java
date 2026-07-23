package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.database.inputs.ProjectContainerInput;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
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
import java.util.concurrent.CountDownLatch;

/** Parses and installs all project inputs while retaining their container ownership. */
public class ClassInputReaderLoadTask extends ProgressiveLoadTask implements ICaption {
    private final ProjectInputSet projectInput;
    private volatile int installedContainerCount;

    public ClassInputReaderLoadTask(ProjectInputSet projectInput) {
        super("Reading Input");
        this.projectInput = projectInput == null ? new ProjectInputSet() : projectInput;
    }

    @Override
    public void runImpl() {
        int classCount = projectInput.getContainers().stream()
                .mapToInt(input -> input.getClassPath().getClasses().size()).sum();
        this.startWork(Math.max(1, classCount));

        Execution execution = getTrinity().getExecution();
        Set<String> reservedNames = execution.getClassList().stream()
                .map(input -> input.getClassTarget().getRealName())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<ParsedContainer> parsedContainers = new ArrayList<>();

        for (ProjectContainerInput input : projectInput.getContainers()) {
            ParsedContainer parsed = parseContainer(input, execution, reservedNames);
            if (parsed != null) parsedContainers.add(parsed);
        }
        if (classCount == 0) this.finishedWork();

        CountDownLatch installed = new CountDownLatch(1);
        Main.runLater(() -> {
            for (ParsedContainer parsed : parsedContainers) installContainer(execution, parsed);
            installedContainerCount = parsedContainers.size();
            installed.countDown();
        });

        try {
            installed.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }

    private ParsedContainer parseContainer(ProjectContainerInput input, Execution execution, Set<String> reservedNames) {
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
            notifyRejected(input.getName(), failure);
            return null;
        }
        reservedNames.addAll(localNames);
        return new ParsedContainer(input, targets);
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
            target.setPackage(container.getRootPackage());
            execution.getClassList().add(target.getInput());
        }
        ProjectContainer installedContainer = container;
        input.getClassPath().getResources().forEach((name, bytes) ->
                new ResourceArchiveEntry(name, bytes,
                        input.getClassPath().getResourceMetadata(name)).setPackage(installedContainer.getRootPackage()));
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

    private record ParsedContainer(ProjectContainerInput input, List<ClassTarget> targets) {
    }
}
