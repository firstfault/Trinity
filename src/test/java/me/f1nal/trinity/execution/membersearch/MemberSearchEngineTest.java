package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.dependency.DependencyManager;
import me.f1nal.trinity.execution.dependency.DependencyArchive;
import me.f1nal.trinity.execution.dependency.DependencyKind;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.util.UnsafeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberSearchEngineTest {
    private Execution execution;
    private MemberSearchEngine engine;
    private FieldInput stringField;

    @BeforeEach
    void createProject() throws Exception {
        execution = emptyExecution();
        Trinity trinity = (Trinity) UnsafeUtil.getUnsafe().allocateInstance(Trinity.class);
        setField(trinity, "execution", execution);

        ClassInput base = install(classNode("sample/Base", "java/lang/Object", Opcodes.ACC_PUBLIC));
        stringField = addField(base, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "message", "Ljava/lang/String;", "Ljava/util/List<Ljava/lang/String;>;");
        addField(base, Opcodes.ACC_PRIVATE, "matrix", "[[I", null);

        ClassInput child = install(classNode("sample/Child", "sample/Base", Opcodes.ACC_PUBLIC));
        MethodNode process = new MethodNode(Opcodes.ACC_PUBLIC, "process",
                "(Ljava/lang/String;I)Ljava/lang/CharSequence;", null, null);
        process.visibleAnnotations = List.of(new AnnotationNode("Lsample/Marker;"));
        process.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        process.instructions.add(new InsnNode(Opcodes.ARETURN));
        addMethod(child, process);

        ClassInput leaf = install(classNode("sample/Leaf", "sample/Child", Opcodes.ACC_PUBLIC));
        addMethod(leaf, new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "unfinished", "()V", null, null));

        ClassInput caller = install(classNode("sample/Caller", "java/lang/Object", Opcodes.ACC_PUBLIC));
        MethodNode read = new MethodNode(Opcodes.ACC_PUBLIC, "read", "()Ljava/lang/String;", null, null);
        read.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "sample/Base", "message", "Ljava/lang/String;"));
        read.instructions.add(new InsnNode(Opcodes.ARETURN));
        addMethod(caller, read);

        execution.getXrefMap().rebuild();
        engine = new MemberSearchEngine(trinity);
    }

    @Test
    void findsTransitiveSubtypesButNeverReturnsTheSelectedBase() {
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.CLASS,
                MemberSearchQuery.Scope.project(), MemberSearchQuery.Common.defaults(),
                new MemberSearchQuery.ClassCriteria(MemberSearchQuery.ClassKind.ANY,
                        new MemberSearchQuery.TypeCriterion("sample.Base",
                                MemberSearchQuery.TypeMode.ASSIGNABLE_TO),
                        MemberSearchQuery.HierarchyDepth.TRANSITIVE),
                MemberSearchQuery.FieldCriteria.defaults(), MemberSearchQuery.MethodCriteria.defaults());

        assertEquals(Set.of("sample.Child", "sample.Leaf"), names(engine.search(query)));
    }

    @Test
    void dependencyClassesParticipateInHierarchyWithoutBecomingResults() throws Exception {
        DependencyArchive archive = new DependencyArchive(UUID.randomUUID(), "api.jar",
                DependencyKind.ARCHIVE, "api.jar", "/tmp/api.jar", null);
        var resolve = DependencyArchive.class.getDeclaredMethod("setResolved", Map.class, String.class);
        resolve.setAccessible(true);
        resolve.invoke(archive, Map.of("dependency/Contract",
                classBytes("dependency/Contract", "java/lang/Object", Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE)),
                "/tmp/api.jar");
        execution.getDependencies().addArchive(archive);

        ClassNode implementation = classNode("sample/Implementation", "java/lang/Object", Opcodes.ACC_PUBLIC);
        implementation.interfaces.add("dependency/Contract");
        install(implementation);

        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.CLASS,
                MemberSearchQuery.Scope.project(), MemberSearchQuery.Common.defaults(),
                new MemberSearchQuery.ClassCriteria(MemberSearchQuery.ClassKind.ANY,
                        new MemberSearchQuery.TypeCriterion("dependency.Contract",
                                MemberSearchQuery.TypeMode.ASSIGNABLE_TO),
                        MemberSearchQuery.HierarchyDepth.TRANSITIVE),
                MemberSearchQuery.FieldCriteria.defaults(), MemberSearchQuery.MethodCriteria.defaults());

        assertEquals(Set.of("sample.Implementation"), names(engine.search(query)));
    }

    @Test
    void fieldSearchCombinesTypeFlagsRenameAndSemanticReferenceState() {
        stringField.getDisplayName().setName("renamedMessage");
        MemberSearchQuery.Common defaults = MemberSearchQuery.Common.defaults();
        MemberSearchQuery.Common common = new MemberSearchQuery.Common(
                new MemberSearchQuery.TextCriterion("message", MemberSearchQuery.TextMode.EXACT, true),
                MemberSearchQuery.Visibility.PUBLIC,
                Map.of(Opcodes.ACC_STATIC, MemberSearchQuery.FlagMode.REQUIRE),
                defaults.ownerKind(), defaults.declaringClass(), defaults.descriptor(),
                defaults.descriptorMode(), "java.lang.String", defaults.annotationType(),
                defaults.annotationLocation(), MemberSearchQuery.RenameState.RENAMED,
                MemberSearchQuery.ReferenceState.REFERENCED, new MemberSearchQuery.IntRange(1, -1));
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.FIELD,
                MemberSearchQuery.Scope.project(), common, MemberSearchQuery.ClassCriteria.defaults(),
                new MemberSearchQuery.FieldCriteria(new MemberSearchQuery.TypeCriterion(
                        "java.lang.String", MemberSearchQuery.TypeMode.EXACT)),
                MemberSearchQuery.MethodCriteria.defaults());

        List<MemberSearchResult> results = engine.search(query);
        assertEquals(1, results.size());
        assertEquals("renamedMessage", results.get(0).name());
        assertEquals(1, results.get(0).referenceCount());
    }

    @Test
    void methodSearchUsesExactParametersAnnotationsAndExecutableInstructionCounts() {
        MemberSearchQuery.Common defaults = MemberSearchQuery.Common.defaults();
        MemberSearchQuery.Common common = new MemberSearchQuery.Common(
                new MemberSearchQuery.TextCriterion("pro.*", MemberSearchQuery.TextMode.REGEX, false),
                defaults.visibility(), defaults.flags(), defaults.ownerKind(), defaults.declaringClass(),
                defaults.descriptor(), defaults.descriptorMode(), defaults.genericType(),
                "sample.Marker", MemberSearchQuery.AnnotationLocation.DECLARATION,
                defaults.renameState(), defaults.referenceState(), defaults.referenceRange());
        MemberSearchQuery.MethodCriteria method = new MemberSearchQuery.MethodCriteria(
                MemberSearchQuery.MethodKind.REGULAR,
                new MemberSearchQuery.TypeCriterion("java.lang.CharSequence", MemberSearchQuery.TypeMode.EXACT),
                new MemberSearchQuery.TypeCriterion("java.lang.String", MemberSearchQuery.TypeMode.EXACT),
                "java.lang.String, int", new MemberSearchQuery.IntRange(2, 2),
                MemberSearchQuery.BodyState.HAS_BODY, new MemberSearchQuery.IntRange(2, 2));
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.METHOD,
                MemberSearchQuery.Scope.project(), common, MemberSearchQuery.ClassCriteria.defaults(),
                MemberSearchQuery.FieldCriteria.defaults(), method);

        List<MemberSearchResult> results = engine.search(query);
        assertEquals(1, results.size());
        assertEquals("process", results.get(0).name());
        assertEquals(2, results.get(0).instructionCount());
    }

    @Test
    void invalidDescriptorsAndRangesAreReportedBeforeSearching() {
        MemberSearchQuery.Common defaults = MemberSearchQuery.Common.defaults();
        MemberSearchQuery.Common common = new MemberSearchQuery.Common(defaults.name(), defaults.visibility(),
                defaults.flags(), defaults.ownerKind(), defaults.declaringClass(), "not-a-descriptor",
                MemberSearchQuery.DescriptorMode.EXACT, defaults.genericType(), defaults.annotationType(),
                defaults.annotationLocation(), defaults.renameState(), defaults.referenceState(),
                new MemberSearchQuery.IntRange(5, 2));
        MemberSearchQuery query = new MemberSearchQuery(MemberSearchQuery.Target.METHOD,
                MemberSearchQuery.Scope.project(), common, MemberSearchQuery.ClassCriteria.defaults(),
                MemberSearchQuery.FieldCriteria.defaults(), MemberSearchQuery.MethodCriteria.defaults());

        List<String> errors = engine.validate(query);
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("Descriptor")));
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("Reference count")));
    }

    private static Set<String> names(List<MemberSearchResult> results) {
        return results.stream().map(MemberSearchResult::name).collect(Collectors.toSet());
    }

    private static Execution emptyExecution() throws Exception {
        Execution execution = (Execution) UnsafeUtil.getUnsafe().allocateInstance(Execution.class);
        setField(execution, "classTargetMap", new HashMap<String, ClassTarget>());
        setField(execution, "classInputList", new ArrayList<ClassInput>());
        setField(execution, "dependencies", new DependencyManager());
        setField(execution, "xrefMap", new XrefMap(execution));
        return execution;
    }

    private ClassInput install(ClassNode node) {
        ClassTarget target = new ClassTarget(node.name, 0);
        ClassInput input = new ClassInput(execution, node, target);
        target.setInput(input);
        execution.addClassTarget(target);
        execution.getClassList().add(input);
        return input;
    }

    private static FieldInput addField(ClassInput owner, int access, String name,
                                       String descriptor, String signature) {
        FieldNode node = new FieldNode(access, name, descriptor, signature, null);
        owner.getNode().fields.add(node);
        FieldInput input = new FieldInput(node, owner);
        owner.addInput(input);
        return input;
    }

    private static MethodInput addMethod(ClassInput owner, MethodNode node) {
        owner.getNode().methods.add(node);
        MethodInput input = new MethodInput(node, owner);
        owner.addInput(input);
        return input;
    }

    private static ClassNode classNode(String name, String superName, int access) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.name = name;
        node.superName = superName;
        node.access = access;
        return node;
    }

    private static byte[] classBytes(String name, String superName, int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, access, name, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void setField(Object instance, String name, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
