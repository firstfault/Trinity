package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.execution.hierarchy.ObjectHierarchyLoadTask;
import me.f1nal.trinity.execution.loading.AsynchronousLoad;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class representing a program's execution flow
 */
public final class Execution {
    private final Map<String, ClassTarget> classTargetMap = new HashMap<>();
    private final List<ClassInput> classInputList = new ArrayList<>();
    private final Map<String, byte[]> resourceMap = new HashMap<>();
    /**
     * Root package (this is the "project" package, with the project input being its children).
     */
    private final Package rootPackage;
    /**
     * Reference map containing all references.
     */
    private final XrefMap xrefMap;
    private final AsynchronousLoad asynchronousLoad;
    private final Trinity trinity;
    private boolean classesLoaded;

    public Execution(Trinity trinity, ClassPath classPath) throws MissingEntryPointException {
        this.trinity = trinity;
        this.rootPackage = new Package(trinity.getDatabase());
        this.xrefMap = new XrefMap(this);

        this.asynchronousLoad = new AsynchronousLoad(this.getTrinity());

        // If this method returned false, we are creating a new database.
        if (!this.trinity.getDatabase().addLoadTasks(this.asynchronousLoad)) {
            if (classPath.getWarnings() != 0) {
                Main.getDisplayManager().addNotification(new Notification(NotificationType.WARNING, new SimpleCaption("Class Path"), ColoredStringBuilder.create()
                        .fmt("Finished reading input with {} warnings", classPath.getWarnings()).get()));
            }
            this.asynchronousLoad.add(new ClassInputReaderLoadTask(classPath.createClassByteList(), classPath.resources));
        }

        this.asynchronousLoad.add(new ObjectHierarchyLoadTask(this));
        this.asynchronousLoad.add(this.xrefMap);
    }

    public void setClassesLoaded() {
        if (this.classesLoaded) {
            return;
        }
        trinity.getEventManager().postEvent(new EventClassesLoaded());
        this.classesLoaded = true;
    }

    public ResourceArchiveEntry createResource(Package pkg, String fileName, byte[] content) {
        String path = pkg.getChildrenPath(fileName);
        if (this.getResourceMap().containsKey(path)) {
            return null;
        }
        this.getResourceMap().put(path, content);
        ResourceArchiveEntry archiveEntry = new ResourceArchiveEntry(path, content);
        archiveEntry.setPackage(this.rootPackage);
        trinity.getEventManager().postEvent(new EventClassesLoaded());
        return archiveEntry;
    }

    public void deleteResource(ResourceArchiveEntry archiveEntry) {
        if (this.getResourceMap().remove(archiveEntry.getRealName()) == null) {
            throw new RuntimeException("This archive entry does not exist so it cannot be removed: " + archiveEntry.getRealName());
        }
        archiveEntry.getPackage().remove(archiveEntry);
        trinity.getEventManager().postEvent(new EventClassesLoaded());
    }

    public void saveResource(ResourceArchiveEntry archiveEntry, byte[] bytes) {
        Package pkg = archiveEntry.getPackage();
        this.deleteResource(archiveEntry);
        this.createResource(pkg, archiveEntry.getRealSimpleName(), bytes);
    }

    public void renameResource(ResourceArchiveEntry archiveEntry, String newName) {
        Package pkg = archiveEntry.getPackage();
        this.deleteResource(archiveEntry);
        this.createResource(pkg, newName, archiveEntry.getBytes());
    }

    public boolean isClassesLoaded() {
        return classesLoaded;
    }

    public @Nullable MethodInput getMethod(MemberDetails details) {
        final ClassInput input = getClassInput(details.getOwner());
        if (input != null) {
            return input.getMethod(details.getName(), details.getDesc());
        }
        return null;
    }

    public @Nullable FieldInput getField(MemberDetails details) {
        final ClassInput input = getClassInput(details.getOwner());
        if (input != null) {
            return input.getField(details.getName(), details.getDesc());
        }
        return null;
    }

    public Package getRootPackage() {
        return rootPackage;
    }

    public @Nullable ClassInput getClassInput(String className) {
        ClassTarget target = getClassTarget(className);
        return target == null ? null : target.getInput();
    }

    public @Nullable ClassTarget getClassTargetByDisplayName(String className) {
        final ClassTarget classTarget = this.getClassTarget(className);

        if (classTarget != null) {
            return classTarget;
        }

        for (ClassTarget target : classTargetMap.values()) {
            if (target.getDisplayOrRealName().equals(className)) {
                return target;
            }
        }

        return null;
    }

    public ClassTarget getClassTarget(String className) {
        return classTargetMap.get(className);
    }

    public ClassTarget addClassTarget(@NotNull String className) {
        final @Nullable ClassTarget classTarget = getClassTarget(className);

        if (classTarget == null) {
            return this.addClassTarget(new ClassTarget(className, 0));
        }

        return classTarget;
    }

    public ClassTarget addClassTarget(ClassTarget classTarget) {
        this.classTargetMap.put(classTarget.getRealName(), classTarget);
        return classTarget;
    }

    /**
     * Retrieves the reference map containing all references.
     *
     * @return The reference map.
     */
    public XrefMap getXrefMap() {
        return xrefMap;
    }

    public List<ClassInput> getClassList() {
        return classInputList;
    }

    public Map<String, ClassTarget> getClassTargetMap() {
        return classTargetMap;
    }

    public AsynchronousLoad getAsynchronousLoad() {
        return asynchronousLoad;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public Map<String, byte[]> getResourceMap() {
        return resourceMap;
    }
}
