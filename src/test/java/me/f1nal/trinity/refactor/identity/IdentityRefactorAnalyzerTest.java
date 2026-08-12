package me.f1nal.trinity.refactor.identity;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.MethodInput;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityRefactorAnalyzerTest {
    @Test
    void requestUsesTheLiveClassfileDeclarationInsteadOfCachedMemberDetails() {
        ClassNode owner = type("sample/Owner", "java/lang/Object");
        ClassTarget target = new ClassTarget(owner.name, 0);
        ClassInput classInput = new ClassInput(null, owner, target);
        target.setInput(classInput);
        MethodNode method = new MethodNode(0, "oldName", "()V", null, null);
        MethodInput input = new MethodInput(method, classInput);

        method.name = "liveName";
        IdentityRefactorRequest request = IdentityRefactorRequest.forInput(
                input, "replacement", false);

        assertEquals("liveName", request.name());
        assertEquals("()V", request.descriptor());
        assertEquals("sample/Owner", request.owner());
    }

    @Test
    void nestedClassDollarNamesAreTreatedAsDistinctBytecodeIdentities() {
        String oldName = "sample/Outer$Inner";
        String newName = "sample/Outer$CompletelyDifferent";
        ClassNode outer = type("sample/Outer", "java/lang/Object");
        ClassNode inner = type(oldName, "java/lang/Object");
        outer.innerClasses = new java.util.ArrayList<>(java.util.List.of(
                new InnerClassNode(oldName, outer.name, "Inner", 0)));
        inner.innerClasses = new java.util.ArrayList<>(java.util.List.of(
                new InnerClassNode(oldName, outer.name, "Inner", 0)));

        Map<String, IdentityRefactorSnapshot.SnapshotClass> project = new LinkedHashMap<>();
        add(project, outer);
        add(project, inner);
        IdentityRefactorPlan plan = new IdentityRefactorAnalyzer().analyze(
                new IdentityRefactorSnapshot(project, Map.of(), 0),
                new IdentityRefactorRequest(IdentityRefactorKind.CLASS,
                        oldName, oldName, null, newName, false));

        assertFalse(plan.hasConflicts());
        assertTrue(plan.getChanges().stream().anyMatch(change ->
                change.before().equals(oldName) && change.after().equals(newName)));
    }

    @Test
    void analysisResolutionDoesNotDependOnDeclarationIterationOrder() {
        ClassNode base = type("sample/Base", "java/lang/Object");
        base.fields.add(new FieldNode(0, "value", "I", null, null));
        ClassNode child = type("sample/Child", "sample/Base");
        ClassNode caller = type("sample/Caller", "java/lang/Object");
        MethodNode method = new MethodNode(Opcodes.ACC_STATIC, "read", "()V", null, null);
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETFIELD, "sample/Child", "value", "I"));
        caller.methods.add(method);

        // Base deliberately appears first. The regression mutated its declaration before
        // resolving the inherited reference in Caller and silently omitted that reference.
        Map<String, IdentityRefactorSnapshot.SnapshotClass> project = new LinkedHashMap<>();
        add(project, base);
        add(project, child);
        add(project, caller);
        IdentityRefactorSnapshot snapshot = new IdentityRefactorSnapshot(project, Map.of(), 0);

        IdentityRefactorPlan plan = new IdentityRefactorAnalyzer().analyze(snapshot,
                new IdentityRefactorRequest(IdentityRefactorKind.FIELD,
                        "sample/Base", "value", "I", "amount", false));

        assertFalse(plan.hasConflicts());
        assertTrue(plan.getChanges().stream().anyMatch(change ->
                change.className().equals("sample/Caller")
                        && change.location().startsWith("Field access name")
                        && change.before().equals("value")
                        && change.after().equals("amount")));
    }

    @Test
    void reflectiveStringsAcrossAnnotationsAndBootstrapArgumentsRequireReview() {
        ClassNode owner = type("sample/Owner", "java/lang/Object");
        owner.methods.add(new MethodNode(0, "work", "()V", null, null));
        ClassNode use = type("sample/Use", "java/lang/Object");
        org.objectweb.asm.tree.AnnotationNode annotation =
                new org.objectweb.asm.tree.AnnotationNode("Lsample/Marker;");
        annotation.values = new java.util.ArrayList<>(java.util.List.of("name", "work"));
        use.visibleAnnotations = new java.util.ArrayList<>(java.util.List.of(annotation));

        Map<String, IdentityRefactorSnapshot.SnapshotClass> project = new LinkedHashMap<>();
        add(project, owner);
        add(project, use);
        IdentityRefactorPlan plan = new IdentityRefactorAnalyzer().analyze(
                new IdentityRefactorSnapshot(project, Map.of(), 0),
                new IdentityRefactorRequest(IdentityRefactorKind.METHOD,
                        "sample/Owner", "work", "()V", "execute", false));

        assertTrue(plan.requiresReview());
        assertTrue(plan.getIssues().stream().anyMatch(issue ->
                issue.title().equals("Possible reflective references")));
    }

    private static void add(Map<String, IdentityRefactorSnapshot.SnapshotClass> project,
                            ClassNode node) {
        ClassTarget target = new ClassTarget(node.name, 0);
        ClassInput input = new ClassInput(null, node, target);
        target.setInput(input);
        project.put(node.name, new IdentityRefactorSnapshot.SnapshotClass(
                input, IdentityRefactorSnapshot.cloneClass(node), input.getBytecodeRevision()));
    }

    private static ClassNode type(String name, String superName) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = 0;
        node.name = name;
        node.superName = superName;
        return node;
    }
}
