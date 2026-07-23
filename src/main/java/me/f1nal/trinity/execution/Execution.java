package me.f1nal.trinity.execution;

import com.google.common.collect.Streams;
import com.google.common.graph.Traverser;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.inputs.ProjectInputSet;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.execution.exception.MissingEntryPointException;
import me.f1nal.trinity.execution.hierarchy.ObjectHierarchyLoadTask;
import me.f1nal.trinity.execution.hierarchy.ClassHierarchy;
import me.f1nal.trinity.execution.loading.AsynchronousLoad;
import me.f1nal.trinity.execution.loading.tasks.ClassInputReaderLoadTask;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ArchiveEntry;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.xref.XrefMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing a program's execution flow
 */
public final class Execution {
    private final Map<String, ClassTarget> classTargetMap = new HashMap<>();
    private final List<ClassInput> classInputList = new ArrayList<>();
    private final List<ProjectContainer> containers = new ArrayList<>();
    /**
     * Reference map containing all references.
     */
    private final XrefMap xrefMap;
    private final AsynchronousLoad asynchronousLoad;
    private final Trinity trinity;
    private boolean classesLoaded;

    public Execution(Trinity trinity, ProjectInputSet projectInput) throws MissingEntryPointException {
        this.trinity = trinity;
        this.xrefMap = new XrefMap(this);

        this.asynchronousLoad = new AsynchronousLoad(this.getTrinity());

        // If this method returned false, we are creating a new database.
        if (!this.trinity.getDatabase().addLoadTasks(this.asynchronousLoad)) {
            this.asynchronousLoad.add(new ClassInputReaderLoadTask(projectInput));
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
        ProjectContainer container = Objects.requireNonNull(pkg.getContainer(), "Package has no project container");
        if (container.getResource(path) != null) {
            return null;
        }
        ResourceArchiveEntry archiveEntry = new ResourceArchiveEntry(path, content);
        archiveEntry.setPackage(container.getRootPackage());
        trinity.getEventManager().postEvent(new EventClassesLoaded());
        return archiveEntry;
    }

    public void deleteResource(ResourceArchiveEntry archiveEntry) {
        if (archiveEntry.getContainer() == null || archiveEntry.getContainer().getResource(archiveEntry.getRealName()) != archiveEntry) {
            throw new RuntimeException("This archive entry does not exist so it cannot be removed: " + archiveEntry.getRealName());
        }
        archiveEntry.getContainer().unregister(archiveEntry);
        archiveEntry.getPackage().remove(archiveEntry);
        trinity.getEventManager().postEvent(new EventClassesLoaded());
    }

    public void saveResource(ResourceArchiveEntry archiveEntry, byte[] bytes) {
        Package pkg = archiveEntry.getPackage();
        var metadata = archiveEntry.getZipMetadata().copy();
        this.deleteResource(archiveEntry);
        ArchiveEntry replacement = this.createResource(pkg, archiveEntry.getRealSimpleName(), bytes);
        if (replacement != null) replacement.setZipMetadata(metadata);
    }

    public void renameResource(ResourceArchiveEntry archiveEntry, String newName) {
        if (newName == null || newName.isBlank()) return;
        ProjectContainer container = archiveEntry.getContainer();
        if (container == null || container.getResource(newName) != null) return;
        Package oldPackage = archiveEntry.getPackage();
        container.unregister(archiveEntry);
        oldPackage.remove(archiveEntry);
        archiveEntry.setName(newName);
        archiveEntry.setPackage(container.getRootPackage());
        trinity.getEventManager().postEvent(new EventClassesLoaded());
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
        return getOrCreateLooseContainer().getRootPackage();
    }

    public List<Package> getAllPackages() {
        Traverser<Package> traverser = Traverser.forTree(Package::getPackages);
        return containers.stream()
                .flatMap(container -> Streams.stream(traverser.depthFirstPreOrder(container.getRootPackage())))
                .collect(Collectors.toList());
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

    public ClassInput createClass(Package pkg, ClassNode classNode) {
        if (getClassTarget(classNode.name) != null) {
            throw new IllegalArgumentException("Class already exists: " + classNode.name);
        }
        String expectedPackage = pkg.getPrettyPath().replace('.', '/');
        int separator = classNode.name.lastIndexOf('/');
        String actualPackage = separator == -1 ? "" : classNode.name.substring(0, separator);
        if (!actualPackage.equals(expectedPackage)) {
            throw new IllegalArgumentException("Class must be created inside package "
                    + (expectedPackage.isEmpty() ? "<default>" : expectedPackage));
        }
        try {
            DataPool.writeClassNode(classNode);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("ASM rejected the new class: " + exception.getMessage(), exception);
        }

        ClassTarget classTarget = new ClassTarget(classNode.name, 0);
        ClassInput classInput = new ClassInput(this, classNode, classTarget);
        classTarget.setInput(classInput);
        classNode.fields.forEach(field -> classInput.addInput(new FieldInput(field, classInput)));
        classNode.methods.forEach(method -> classInput.addInput(new MethodInput(method, classInput)));
        addClassTarget(classTarget);
        classInputList.add(classInput);
        classTarget.setPackage(pkg.getContainer().getRootPackage());
        trinity.getEventManager().postEvent(new EventClassesLoaded());
        return classInput;
    }

    public void reindexClass(ClassInput classInput, String newName) {
        ClassTarget target = classInput.getClassTarget();
        String oldName = target.getRealName();
        if (oldName.equals(newName)) {
            return;
        }
        ClassTarget conflict = getClassTarget(newName);
        if (conflict != null && conflict != target) {
            throw new IllegalArgumentException("Class already exists: " + newName);
        }

        classTargetMap.remove(oldName);
        classInput.getNode().name = newName;
        classInput.markRebuildRequired();
        target.replaceRealName(newName);
        classInput.reindexDeclaredMembers();
        classTargetMap.put(newName, target);
        ProjectContainer container = target.getContainer();
        target.setPackage(container == null ? getRootPackage() : container.getRootPackage());
        trinity.getEventManager().postEvent(new EventClassesLoaded());
    }

    /** Refreshes hierarchy-dependent member lookup and cross-reference targets. */
    public void refreshStructuralIndexes() {
        ClassHierarchy.rebuildAll(this);
        xrefMap.rebuild();
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

    public List<ProjectContainer> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    public void addContainer(ProjectContainer container) {
        if (containers.stream().anyMatch(existing -> existing.getId().equals(container.getId()))) {
            throw new IllegalArgumentException("Duplicate project container ID: " + container.getId());
        }
        containers.add(container);
    }

    public void removeContainer(ProjectContainer container) {
        if (!containers.remove(container)) return;
        for (ClassTarget target : new ArrayList<>(container.getClasses())) {
            classTargetMap.remove(target.getRealName(), target);
            if (target.getInput() != null) classInputList.remove(target.getInput());
            container.unregister(target);
            if (target.getPackage() != null) target.getPackage().remove(target);
        }
        for (ResourceArchiveEntry resource : new ArrayList<>(container.getResources())) {
            container.unregister(resource);
            if (resource.getPackage() != null) resource.getPackage().remove(resource);
        }
        trinity.getEventManager().postEvent(new EventClassesLoaded());
    }

    public ProjectContainer getContainer(UUID id) {
        return containers.stream().filter(container -> container.getId().equals(id)).findFirst().orElse(null);
    }

    public ProjectContainer getOrCreateLooseContainer() {
        ProjectContainer loose = containers.stream()
                .filter(container -> container.getKind() == ProjectContainerKind.LOOSE)
                .findFirst().orElse(null);
        if (loose == null) {
            loose = new ProjectContainer(UUID.randomUUID(), ProjectInputSet.LOOSE_FILES_NAME,
                    ProjectContainerKind.LOOSE, trinity.getDatabase());
            containers.add(loose);
        }
        return loose;
    }

    public Collection<ResourceArchiveEntry> getResources() {
        return containers.stream().flatMap(container -> container.getResources().stream()).toList();
    }
}
