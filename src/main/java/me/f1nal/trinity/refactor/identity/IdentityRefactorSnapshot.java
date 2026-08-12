package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class IdentityRefactorSnapshot {
    private final Map<String, SnapshotClass> projectClasses;
    private final Map<String, byte[]> dependencyClasses;
    private final int unresolvedDependencies;
    private final IdentityProjectState projectState;

    IdentityRefactorSnapshot(Map<String, SnapshotClass> projectClasses,
                             Map<String, byte[]> dependencyClasses,
                             int unresolvedDependencies) {
        this(projectClasses, dependencyClasses, unresolvedDependencies,
                IdentityProjectState.detached(revisions(projectClasses)));
    }

    private IdentityRefactorSnapshot(Map<String, SnapshotClass> projectClasses,
                                     Map<String, byte[]> dependencyClasses,
                                     int unresolvedDependencies,
                                     IdentityProjectState projectState) {
        this.projectClasses = projectClasses;
        this.dependencyClasses = dependencyClasses;
        this.unresolvedDependencies = unresolvedDependencies;
        this.projectState = projectState;
    }

    static IdentityRefactorSnapshot capture(Trinity trinity) {
        Map<String, SnapshotClass> project = new LinkedHashMap<>();
        for (ClassInput input : trinity.getExecution().getClassList()) {
            project.put(input.getRealName(), new SnapshotClass(
                    input, cloneClass(input.getNode()), input.getBytecodeRevision()));
        }

        Map<String, byte[]> dependencies = new LinkedHashMap<>();
        int unresolved = 0;
        for (DependencyArchive archive : trinity.getExecution().getDependencies().getArchives()) {
            if (!archive.isResolved()) {
                unresolved++;
                continue;
            }
            for (Map.Entry<String, byte[]> entry : archive.getClasses().entrySet()) {
                if (project.containsKey(entry.getKey())) continue;
                // Archive maps and byte arrays are immutable after resolution. Retaining their
                // references makes snapshotting cheap; hierarchy classes are parsed lazily.
                dependencies.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return new IdentityRefactorSnapshot(project, dependencies, unresolved,
                IdentityProjectState.capture(trinity));
    }

    static ClassNode cloneClass(ClassNode source) {
        ClassNode clone = new ClassNode(Opcodes.ASM9);
        source.accept(clone);
        return clone;
    }

    Map<String, SnapshotClass> projectClasses() {
        return projectClasses;
    }

    Map<String, byte[]> dependencyClasses() {
        return dependencyClasses;
    }

    int unresolvedDependencies() {
        return unresolvedDependencies;
    }

    IdentityProjectState projectState() {
        return projectState;
    }

    private static Map<ClassInput, Long> revisions(Map<String, SnapshotClass> classes) {
        Map<ClassInput, Long> revisions = new IdentityHashMap<>();
        classes.values().forEach(snapshot -> {
            if (snapshot.input() != null) revisions.put(snapshot.input(), snapshot.revision());
        });
        return revisions;
    }

    record SnapshotClass(ClassInput input, ClassNode node, long revision) {
    }
}
