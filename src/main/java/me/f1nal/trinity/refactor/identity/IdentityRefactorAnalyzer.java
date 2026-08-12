package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.constant.AsmConstantScanner;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds and validates complete classfile rename plans without touching the live project. */
final class IdentityRefactorAnalyzer {
    IdentityRefactorPlan analyze(IdentityRefactorSnapshot snapshot,
                                 IdentityRefactorRequest request) {
        List<IdentityRefactorIssue> issues = new ArrayList<>();
        IdentityClassUniverse universe = new IdentityClassUniverse(
                snapshot.projectClasses(), snapshot.dependencyClasses());
        IdentityMapping mapping = buildMapping(snapshot, universe, request, issues);
        List<IdentityRefactorChange> changes = new ArrayList<>();
        Set<ClassInput> affected = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        IdentityAsmRemapper remapper = new IdentityAsmRemapper(mapping, universe);
        Map<String, ClassNode> transformedClasses = new LinkedHashMap<>();
        for (Map.Entry<String, IdentityRefactorSnapshot.SnapshotClass> entry
                : snapshot.projectClasses().entrySet()) {
            int previousChangeCount = changes.size();
            // Keep the universe immutable while transforming. Member resolution for a
            // later class must still see the old declarations even if their owner was
            // already visited by this analysis pass.
            ClassNode node = IdentityRefactorSnapshot.cloneClass(entry.getValue().node());
            new IdentityClassTransformer(remapper, changes).transform(node);
            transformedClasses.put(node.name, node);
            if (changes.size() != previousChangeCount) {
                if (entry.getValue().input() != null) affected.add(entry.getValue().input());
            }
        }

        long opaqueAttributeClasses = snapshot.projectClasses().values().stream()
                .filter(value -> containsCustomAttributes(value.node())).count();
        if (opaqueAttributeClasses != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Custom classfile attributes",
                    opaqueAttributeClasses + " project class"
                            + (opaqueAttributeClasses == 1 ? " contains" : "es contain")
                            + " opaque attribute data that cannot be inspected or remapped.");
        }

        validateTransformedClasses(snapshot, transformedClasses, affected, mapping, issues);
        findPossibleStringReferences(snapshot, request, remapper, issues);
        if (universe.unreadableDependencyCount() != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Unreadable dependency classes",
                    universe.unreadableDependencyCount() + " dependency class"
                            + (universe.unreadableDependencyCount() == 1 ? " could" : "es could")
                            + " not be parsed while resolving references. Hierarchy analysis may be incomplete.");
        }
        if (snapshot.unresolvedDependencies() != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Unresolved dependencies",
                    snapshot.unresolvedDependencies() + " dependency archive"
                            + (snapshot.unresolvedDependencies() == 1 ? " is" : "s are")
                            + " unresolved. External hierarchy and member resolution may be incomplete.");
        }
        if (changes.isEmpty() && issues.stream().noneMatch(issue ->
                issue.severity() == IdentityRefactorSeverity.CONFLICT)) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Nothing to rename", "The requested identity was not found in the current project.");
        }

        return new IdentityRefactorPlan(request, changes, issues, mapping,
                snapshot.projectState(), affected);
    }

    private IdentityMapping buildMapping(IdentityRefactorSnapshot snapshot,
                                         IdentityClassUniverse universe,
                                         IdentityRefactorRequest request,
                                         List<IdentityRefactorIssue> issues) {
        Map<String, String> classes = new LinkedHashMap<>();
        Set<IdentityMemberKey> methods = new LinkedHashSet<>();
        Set<IdentityMemberKey> fields = new LinkedHashSet<>();
        Set<IdentityMemberKey> records = new LinkedHashSet<>();

        switch (request.kind()) {
            case CLASS -> buildClassMapping(snapshot, request, classes, issues);
            case METHOD -> buildMethodMapping(snapshot, universe, request, methods, issues);
            case FIELD -> buildFieldMapping(snapshot, universe, request,
                    methods, fields, records, issues);
        }
        return new IdentityMapping(classes, methods, fields, records, request.newName());
    }

    private void buildClassMapping(IdentityRefactorSnapshot snapshot,
                                   IdentityRefactorRequest request,
                                   Map<String, String> classes,
                                   List<IdentityRefactorIssue> issues) {
        if (!validInternalName(request.newName())) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Invalid class name",
                    "Use a non-empty slash-separated JVM internal name without '.', ';', '[' or empty path segments.");
            return;
        }
        IdentityRefactorSnapshot.SnapshotClass target = snapshot.projectClasses().get(request.owner());
        if (target == null) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Class no longer exists", request.owner() + " is not present in the project.");
            return;
        }
        if (request.owner().equals(request.newName())) return;
        if (snapshot.projectClasses().containsKey(request.newName())) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Class name collision", request.newName() + " already exists in the project.");
        }
        if (snapshot.dependencyClasses().containsKey(request.newName())) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Dependency class collision",
                    request.newName() + " already exists on the dependency classpath.");
        }
        classes.put(request.owner(), request.newName());
        ClassNode node = target.node();
        if ((node.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Externally visible class",
                    "References from applications or libraries outside this project cannot be updated.");
        }
        if (!packageName(request.owner()).equals(packageName(request.newName()))) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Package move",
                    "Moving a class can change package-private access and override relationships. "
                            + "Package-level module exports and opens are not moved automatically.");
        }
    }

    private void buildMethodMapping(IdentityRefactorSnapshot snapshot,
                                    IdentityClassUniverse universe,
                                    IdentityRefactorRequest request,
                                    Set<IdentityMemberKey> methods,
                                    List<IdentityRefactorIssue> issues) {
        if (!validMemberName(request.newName(), true)) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Invalid method name", "The proposed value is not a valid JVM method name.");
            return;
        }
        IdentityRefactorSnapshot.SnapshotClass owner = snapshot.projectClasses().get(request.owner());
        MethodNode selected = owner == null ? null : IdentityClassUniverse.declaredMethod(
                owner.node(), request.name(), request.descriptor());
        if (selected == null) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Method no longer exists", request.oldIdentity() + " is not present in the project.");
            return;
        }
        if (selected.name.equals("<init>") || selected.name.equals("<clinit>")) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "JVM special method", "Constructors and class initializers cannot be renamed as methods.");
            return;
        }
        IdentityMemberKey target = new IdentityMemberKey(
                request.owner(), request.name(), request.descriptor());
        methods.addAll(universe.projectOverrideFamily(target));
        for (IdentityMemberKey member : methods) {
            IdentityRefactorSnapshot.SnapshotClass memberOwner = snapshot.projectClasses().get(member.owner());
            MethodNode conflict = IdentityClassUniverse.declaredMethod(
                    memberOwner.node(), request.newName(), member.descriptor());
            if (conflict != null && !conflict.name.equals(member.name())) {
                addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                        "Method name collision",
                        member.owner() + " already declares " + request.newName() + member.descriptor() + '.');
            }
        }
        warnAboutIntroducedMethodRelationships(snapshot, universe, methods,
                request.newName(), issues);
        if (methods.stream().anyMatch(method -> universe.hasDependencyContract(method))) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Dependency method contract",
                    "This method overrides or implements a dependency method. The dependency declaration cannot be renamed.");
        }
        if ((selected.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Externally visible method",
                    "Callers outside this project cannot be updated.");
        }
        if ((selected.access & Opcodes.ACC_NATIVE) != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Native method", "JNI bindings may depend on the original method name.");
        }
    }

    private void buildFieldMapping(IdentityRefactorSnapshot snapshot,
                                   IdentityClassUniverse universe,
                                   IdentityRefactorRequest request,
                                   Set<IdentityMemberKey> methods,
                                   Set<IdentityMemberKey> fields,
                                   Set<IdentityMemberKey> records,
                                   List<IdentityRefactorIssue> issues) {
        if (!validMemberName(request.newName(), false)) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Invalid field name", "The proposed value is not a valid JVM field name.");
            return;
        }
        IdentityRefactorSnapshot.SnapshotClass owner = snapshot.projectClasses().get(request.owner());
        FieldNode selected = owner == null ? null : IdentityClassUniverse.declaredField(
                owner.node(), request.name(), request.descriptor());
        if (selected == null) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Field no longer exists", request.oldIdentity() + " is not present in the project.");
            return;
        }
        if (IdentityClassUniverse.declaredField(owner.node(), request.newName(), request.descriptor()) != null
                && !request.newName().equals(request.name())) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Field name collision",
                    request.owner() + " already declares " + request.newName() + ' ' + request.descriptor() + '.');
        }
        IdentityMemberKey field = new IdentityMemberKey(
                request.owner(), request.name(), request.descriptor());
        fields.add(field);
        warnAboutIntroducedFieldRelationships(
                snapshot, universe, field, request.newName(), issues);
        if ((selected.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Externally visible field", "Accesses outside this project cannot be updated.");
        }
        if ((selected.access & Opcodes.ACC_ENUM) != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Enum constant",
                    "Enum.name(), serialized values, reflection, and configuration may depend on the original name.");
        }

        if (owner.node().recordComponents != null) {
            for (RecordComponentNode component : owner.node().recordComponents) {
                if (!component.name.equals(request.name())
                        || !component.descriptor.equals(request.descriptor())) continue;
                records.add(field);
                String accessorDescriptor = "()" + request.descriptor();
                MethodNode accessor = IdentityClassUniverse.declaredMethod(
                        owner.node(), request.name(), accessorDescriptor);
                if (accessor != null) {
                    if (IdentityClassUniverse.declaredMethod(owner.node(),
                            request.newName(), accessorDescriptor) != null
                            && !request.newName().equals(request.name())) {
                        addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                                "Record accessor collision",
                                request.owner() + " already declares "
                                        + request.newName() + accessorDescriptor + '.');
                    }
                    methods.add(new IdentityMemberKey(
                            request.owner(), request.name(), accessorDescriptor));
                }
                addIssue(issues, IdentityRefactorSeverity.INFORMATION,
                        "Record component family",
                        "The record component, backing field, and accessor will be renamed together.");
            }
        }
        if (!methods.isEmpty()) {
            warnAboutIntroducedMethodRelationships(
                    snapshot, universe, methods, request.newName(), issues);
        }
    }

    private void warnAboutIntroducedMethodRelationships(
            IdentityRefactorSnapshot snapshot,
            IdentityClassUniverse universe,
            Set<IdentityMemberKey> renamedMethods,
            String newName,
            List<IdentityRefactorIssue> issues) {
        Set<String> relationships = new LinkedHashSet<>();
        for (IdentityMemberKey renamed : renamedMethods) {
            for (IdentityMemberKey inherited : universe.resolveMethods(
                    renamed.owner(), newName, renamed.descriptor())) {
                if (!inherited.owner().equals(renamed.owner())) {
                    relationships.add(renamed.owner() + " -> " + inherited.display());
                }
            }
            snapshot.projectClasses().forEach((owner, value) -> {
                if (owner.equals(renamed.owner())) return;
                if (IdentityClassUniverse.declaredMethod(
                        value.node(), newName, renamed.descriptor()) == null) return;
                if (universe.isSubtype(owner, renamed.owner())
                        || universe.isSubtype(renamed.owner(), owner)) {
                    relationships.add(renamed.owner() + " <-> " + owner + '.'
                            + newName + renamed.descriptor());
                }
            });
        }
        if (!relationships.isEmpty()) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "New override relationship",
                    "The new name matches " + relationships.size()
                            + " inherited or descendant declaration"
                            + (relationships.size() == 1 ? "" : "s")
                            + ". Dynamic dispatch behavior may change (for example, "
                            + relationships.iterator().next() + ").");
        }
    }

    private void warnAboutIntroducedFieldRelationships(
            IdentityRefactorSnapshot snapshot,
            IdentityClassUniverse universe,
            IdentityMemberKey renamedField,
            String newName,
            List<IdentityRefactorIssue> issues) {
        Set<String> relationships = new LinkedHashSet<>();
        IdentityMemberKey inherited = universe.resolveField(
                renamedField.owner(), newName, renamedField.descriptor());
        if (inherited != null && !inherited.owner().equals(renamedField.owner())) {
            relationships.add(renamedField.owner() + " -> " + inherited.display());
        }
        snapshot.projectClasses().forEach((owner, value) -> {
            if (owner.equals(renamedField.owner())) return;
            if (IdentityClassUniverse.declaredField(
                    value.node(), newName, renamedField.descriptor()) == null) return;
            if (universe.isSubtype(owner, renamedField.owner())
                    || universe.isSubtype(renamedField.owner(), owner)) {
                relationships.add(renamedField.owner() + " <-> " + owner + '.'
                        + newName + ' ' + renamedField.descriptor());
            }
        });
        if (!relationships.isEmpty()) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "New field hiding relationship",
                    "The new name matches " + relationships.size()
                            + " inherited or descendant field"
                            + (relationships.size() == 1 ? "" : "s")
                            + ". Symbolic field resolution may change (for example, "
                            + relationships.iterator().next() + ").");
        }
    }

    private void validateTransformedClasses(
            IdentityRefactorSnapshot snapshot,
            Map<String, ClassNode> transformed,
            Set<ClassInput> affected,
            IdentityMapping mapping,
            List<IdentityRefactorIssue> issues) {
        if (transformed.size() != snapshot.projectClasses().size()) {
            addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                    "Duplicate class identity", "Two project classes would have the same internal name.");
        }
        for (ClassInput input : affected) {
            ClassNode node = transformed.get(mapping.classes().getOrDefault(
                    input.getRealName(), input.getRealName()));
            if (node == null) continue;
            validateMemberDuplicates(node, issues);
            try {
                node.check(Opcodes.ASM9);
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                writer.toByteArray();
            } catch (Throwable throwable) {
                addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                        "Invalid transformed class",
                        node.name + " could not be serialized: " + message(throwable));
            }
        }
    }

    private void validateMemberDuplicates(ClassNode node, List<IdentityRefactorIssue> issues) {
        Set<String> methods = new HashSet<>();
        for (MethodNode method : node.methods) {
            if (!methods.add(method.name + method.desc)) {
                addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                        "Duplicate method after remapping", node.name + '.' + method.name + method.desc);
            }
        }
        Set<String> fields = new HashSet<>();
        for (FieldNode field : node.fields) {
            if (!fields.add(field.name + '\u0000' + field.desc)) {
                addIssue(issues, IdentityRefactorSeverity.CONFLICT,
                        "Duplicate field after remapping", node.name + '.' + field.name + ' ' + field.desc);
            }
        }
    }

    private void findPossibleStringReferences(IdentityRefactorSnapshot snapshot,
                                              IdentityRefactorRequest request,
                                              IdentityAsmRemapper remapper,
                                              List<IdentityRefactorIssue> issues) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(request.kind() == IdentityRefactorKind.CLASS
                ? request.owner() : request.name());
        if (request.kind() == IdentityRefactorKind.CLASS) {
            candidates.add(request.owner().replace('/', '.'));
            candidates.add('L' + request.owner() + ';');
        }
        int count = 0;
        for (IdentityRefactorSnapshot.SnapshotClass snapshotClass : snapshot.projectClasses().values()) {
            for (AsmConstantScanner.Occurrence occurrence
                    : AsmConstantScanner.scan(snapshotClass.node())) {
                if (occurrence.value() instanceof String text && candidates.contains(text)
                        && !isMappedBootstrapString(occurrence, text, remapper)) {
                    count++;
                }
            }
        }
        if (count != 0) {
            addIssue(issues, IdentityRefactorSeverity.WARNING,
                    "Possible reflective references",
                    count + " exact string constant" + (count == 1 ? " matches" : "s match")
                            + " the old name in bytecode or annotations. Strings are not changed automatically.");
        }
    }

    private static boolean isMappedBootstrapString(
            AsmConstantScanner.Occurrence occurrence,
            String text,
            IdentityAsmRemapper remapper) {
        if (!(occurrence.site().instruction() instanceof InvokeDynamicInsnNode dynamic)
                || dynamic.bsmArgs == null) return false;
        Object[] arguments = dynamic.bsmArgs.clone();
        for (int index = 0; index < arguments.length; index++) {
            if (!text.equals(arguments[index])) continue;
            Object mapped = remapper.mapBootstrapArgument(
                    dynamic.bsm, dynamic.desc, arguments, index, arguments[index]);
            if (!text.equals(mapped)) return true;
        }
        return false;
    }

    private static boolean containsCustomAttributes(ClassNode node) {
        if (node.attrs != null && !node.attrs.isEmpty()) return true;
        if (node.recordComponents != null && node.recordComponents.stream()
                .anyMatch(component -> component.attrs != null && !component.attrs.isEmpty())) return true;
        if (node.fields != null && node.fields.stream()
                .anyMatch(field -> field.attrs != null && !field.attrs.isEmpty())) return true;
        return node.methods != null && node.methods.stream()
                .anyMatch(method -> method.attrs != null && !method.attrs.isEmpty());
    }

    private static boolean validInternalName(String name) {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.endsWith("/")
                || name.contains("//")) return false;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (character == '.' || character == ';' || character == '[' || character == '\u0000') {
                return false;
            }
        }
        return true;
    }

    private static boolean validMemberName(String name, boolean method) {
        if (name == null || name.isEmpty()) return false;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (character == '.' || character == ';' || character == '['
                    || character == '/' || character == '\u0000') return false;
            if (method && (character == '<' || character == '>')) return false;
        }
        return true;
    }

    private static String packageName(String owner) {
        int separator = owner.lastIndexOf('/');
        return separator == -1 ? "" : owner.substring(0, separator);
    }

    private static void addIssue(List<IdentityRefactorIssue> issues,
                                 IdentityRefactorSeverity severity,
                                 String title, String detail) {
        IdentityRefactorIssue issue = new IdentityRefactorIssue(severity, title, detail);
        if (!issues.contains(issue)) issues.add(issue);
    }

    private static String message(Throwable throwable) {
        return throwable.getMessage() == null
                ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
