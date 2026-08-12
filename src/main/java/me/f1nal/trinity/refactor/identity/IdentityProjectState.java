package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.RenameType;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Lightweight live-state guard retained after the analysis snapshot is discarded. */
final class IdentityProjectState {
    private final Trinity trinity;
    private final Map<ClassInput, ClassState> classes;
    private final Map<DisplayName, DisplayState> displayNames;
    private final List<DependencyState> dependencies;

    private IdentityProjectState(Trinity trinity,
                                 Map<ClassInput, ClassState> classes,
                                 Map<DisplayName, DisplayState> displayNames,
                                 List<DependencyState> dependencies) {
        this.trinity = trinity;
        this.classes = classes;
        this.displayNames = displayNames;
        this.dependencies = dependencies;
    }

    static IdentityProjectState capture(Trinity trinity) {
        Map<ClassInput, ClassState> classes = new IdentityHashMap<>();
        Map<DisplayName, DisplayState> names = new IdentityHashMap<>();
        for (ClassInput input : trinity.getExecution().getClassList()) {
            classes.put(input, new ClassState(input.getBytecodeRevision(),
                    input.getRealName(), input.getNode().name));
            captureName(names, input.getDisplayName());
            for (MemberInput<?> member : input.getMemberList()) {
                captureName(names, member.getDisplayName());
            }
        }
        List<DependencyState> dependencies = trinity.getExecution().getDependencies()
                .getArchives().stream().map(DependencyState::capture).toList();
        return new IdentityProjectState(trinity, classes, names, dependencies);
    }

    static IdentityProjectState detached(Map<ClassInput, Long> revisions) {
        Map<ClassInput, ClassState> classes = new IdentityHashMap<>();
        revisions.forEach((input, revision) -> {
            if (input != null) classes.put(input, new ClassState(revision,
                    input.getRealName(), input.getNode().name));
        });
        return new IdentityProjectState(null, classes, Map.of(), List.of());
    }

    boolean isCurrent() {
        if (trinity == null) {
            return classes.entrySet().stream().allMatch(entry ->
                    entry.getKey().getBytecodeRevision() == entry.getValue().revision());
        }
        List<ClassInput> currentClasses = trinity.getExecution().getClassList();
        if (currentClasses.size() != classes.size()) return false;
        for (ClassInput input : currentClasses) {
            ClassState expected = classes.get(input);
            if (expected == null || input.getBytecodeRevision() != expected.revision()
                    || !input.getRealName().equals(expected.realName())
                    || !input.getNode().name.equals(expected.nodeName())) return false;
        }
        for (Map.Entry<DisplayName, DisplayState> entry : displayNames.entrySet()) {
            DisplayName name = entry.getKey();
            DisplayState expected = entry.getValue();
            if (!name.getOriginalName().equals(expected.originalName())
                    || !name.getName().equals(expected.name())
                    || name.getType() != expected.type()) return false;
        }

        List<DependencyArchive> currentDependencies =
                trinity.getExecution().getDependencies().getArchives();
        if (currentDependencies.size() != dependencies.size()) return false;
        for (int index = 0; index < dependencies.size(); index++) {
            if (!dependencies.get(index).matches(currentDependencies.get(index))) return false;
        }
        return true;
    }

    boolean belongsTo(Trinity project) {
        return trinity == null || trinity == project;
    }

    private static void captureName(Map<DisplayName, DisplayState> output, DisplayName name) {
        output.put(name, new DisplayState(
                name.getOriginalName(), name.getName(), name.getType()));
    }

    private record ClassState(long revision, String realName, String nodeName) {
    }

    private record DisplayState(String originalName, String name, RenameType type) {
    }

    private record DependencyState(DependencyArchive archive, boolean resolved,
                                   Map<String, byte[]> classMap, String error) {
        static DependencyState capture(DependencyArchive archive) {
            return new DependencyState(archive, archive.isResolved(),
                    archive.getClasses(), archive.getResolutionError());
        }

        boolean matches(DependencyArchive current) {
            return current == archive && current.isResolved() == resolved
                    && current.getClasses() == classMap
                    && java.util.Objects.equals(current.getResolutionError(), error);
        }
    }
}
