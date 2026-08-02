package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.MemberDetails;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleProvideNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsmReferenceScannerTest {
    private static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            "bootstrap/Owner",
            "bootstrap",
            "(Lbootstrap/Argument;)Lbootstrap/Return;",
            false);
    private static final Handle FIELD_HANDLE = new Handle(
            Opcodes.H_GETSTATIC,
            "handle/Owner",
            "VALUE",
            "Lhandle/Value;",
            false);

    @Test
    void extractsDeclarationDescriptorsAndGenericSignaturesRecursively() {
        ClassNode node = baseClass();
        node.signature = "<T:Lsignature/Bound;>Ljava/util/ArrayList<Lsignature/Element;>;"
                + "Lsignature/Interface;";

        FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE, "values",
                "[[Lfield/Element;",
                "Lsignature/Outer<Lsignature/Argument;>.Inner<Lsignature/InnerArgument;>;",
                null);
        node.fields.add(field);

        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "convert",
                "([Lmethod/Argument;)[[Lmethod/Return;",
                "<T:Lmethod/Bound;>(TT;Ljava/util/List<+Lmethod/GenericArgument;>;)"
                        + "[Lmethod/GenericReturn;^Lmethod/GenericException;",
                new String[]{"method/DeclaredException"});
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(end);
        method.localVariables = List.of(new LocalVariableNode(
                "local", "Llocal/Descriptor;", "Ljava/util/List<Llocal/Generic;>;",
                start, end, 1));
        node.methods.add(method);

        AsmReferenceScanner.ScanResult result = AsmReferenceScanner.scanClass(node);
        Set<String> owners = owners(result);

        assertContainsAll(owners,
                "signature/Bound", "java/util/ArrayList", "signature/Element",
                "signature/Interface", "field/Element", "signature/Outer",
                "signature/Argument", "signature/Outer$Inner", "signature/InnerArgument",
                "method/Argument", "method/Return", "method/Bound", "java/util/List",
                "method/GenericArgument", "method/GenericReturn",
                "method/GenericException", "method/DeclaredException",
                "local/Descriptor", "local/Generic");
        assertTrue(result.classReferences().stream().anyMatch(reference ->
                reference.owner().equals("method/Return")
                        && reference.kind() == XrefKind.RETURN));
        assertTrue(result.classReferences().stream().anyMatch(reference ->
                reference.owner().equals("method/Argument")
                        && reference.kind() == XrefKind.PARAMETER));
        assertReference(result, "signature/Bound", XrefKind.METADATA, "Class signature");
        assertReference(result, "signature/Outer", XrefKind.METADATA, "Field signature");
        assertReference(result, "method/Bound", XrefKind.METADATA, "Method signature");
        assertReference(result, "field/Element", XrefKind.TYPE, "Field type");
        assertReference(result, "local/Descriptor", XrefKind.VARIABLE, "Variable");
        assertReference(result, "local/Generic", XrefKind.VARIABLE, "Variable");
    }

    @Test
    void extractsAnnotationsRecordsAndStructuralMetadata() {
        ClassNode node = baseClass();
        node.outerClass = "metadata/Outer";
        node.outerMethod = "create";
        node.outerMethodDesc = "(Lmetadata/OuterArgument;)Lmetadata/OuterReturn;";
        node.nestHostClass = "metadata/NestHost";
        node.nestMembers = List.of("metadata/NestMember");
        node.permittedSubclasses = List.of("metadata/Permitted");
        node.innerClasses = List.of(new InnerClassNode(
                "metadata/Outer$Inner", "metadata/Outer", "Inner", Opcodes.ACC_PUBLIC));

        ModuleNode module = new ModuleNode("fixture.module", 0, null);
        module.mainClass = "module/Main";
        module.uses = List.of("module/Service");
        module.provides = List.of(new ModuleProvideNode(
                "module/ProvidedService", List.of("module/Provider")));
        node.module = module;

        node.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTypeReference(TypeReference.CLASS_EXTENDS).getValue(),
                "Lannotation/Class;", Type.getType("Lannotation/ClassValue;")));
        AnnotationNode nested = annotation(
                "Lannotation/Nested;", Type.getType("Lannotation/NestedValue;"));
        AnnotationNode enumAnnotation = new AnnotationNode("Lannotation/Container;");
        enumAnnotation.values = new ArrayList<>(List.of(
                "nested", nested,
                "choice", new String[]{"Lannotation/Enum;", "ENTRY"}));
        node.visibleAnnotations = List.of(enumAnnotation);

        RecordComponentNode record = new RecordComponentNode(
                "component", "Lrecord/Descriptor;",
                "Ljava/util/List<Lrecord/Generic;>;");
        record.visibleAnnotations = List.of(annotation(
                "Lannotation/Record;", Type.getType("Lannotation/RecordValue;")));
        node.recordComponents = List.of(record);

        FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE, "field",
                "Ljava/lang/Object;", null, null);
        field.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                "Lannotation/Field;", Type.getType("Lannotation/FieldValue;")));
        node.fields.add(field);

        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "annotated", "()V", null, null);
        method.annotationDefault = Type.getType("Lannotation/DefaultValue;");
        method.visibleParameterAnnotations = parameterAnnotations(annotation(
                "Lannotation/Parameter;", Type.getType("Lannotation/ParameterValue;")));
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.add(start);
        LdcInsnNode instruction = new LdcInsnNode("value");
        instruction.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0).getValue(),
                "Lannotation/Instruction;",
                Type.getType("Lannotation/InstructionValue;")));
        method.instructions.add(instruction);
        method.instructions.add(end);
        method.instructions.add(handler);

        LocalVariableAnnotationNode localAnnotation = new LocalVariableAnnotationNode(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(),
                null, new LabelNode[]{start}, new LabelNode[]{end}, new int[]{1},
                "Lannotation/Local;");
        localAnnotation.values = values(Type.getType("Lannotation/LocalValue;"));
        method.visibleLocalVariableAnnotations = List.of(localAnnotation);

        TryCatchBlockNode tryCatch = new TryCatchBlockNode(
                start, end, handler, "exception/Caught");
        tryCatch.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTryCatchReference(0).getValue(),
                "Lannotation/TryCatch;", Type.getType("Lannotation/TryCatchValue;")));
        method.tryCatchBlocks = List.of(tryCatch);
        node.methods.add(method);

        AsmReferenceScanner.ScanResult result = AsmReferenceScanner.scanClass(node);
        Set<String> owners = owners(result);

        assertContainsAll(owners,
                "metadata/Outer", "metadata/OuterArgument", "metadata/OuterReturn",
                "metadata/NestHost", "metadata/NestMember", "metadata/Permitted",
                "metadata/Outer$Inner", "module/Main", "module/Service",
                "module/ProvidedService", "module/Provider",
                "annotation/Class", "annotation/ClassValue",
                "annotation/Container", "annotation/Nested",
                "annotation/NestedValue", "annotation/Enum",
                "record/Descriptor", "record/Generic", "annotation/Record",
                "annotation/RecordValue", "annotation/Field", "annotation/FieldValue",
                "annotation/DefaultValue", "annotation/Parameter",
                "annotation/ParameterValue", "annotation/Instruction",
                "annotation/InstructionValue", "annotation/Local",
                "annotation/LocalValue", "exception/Caught",
                "annotation/TryCatch", "annotation/TryCatchValue");
        assertReference(result, "metadata/Outer", XrefKind.METADATA, "Enclosing class");
        assertReference(result, "metadata/OuterArgument", XrefKind.METADATA,
                "Enclosing method descriptor");
        assertReference(result, "metadata/OuterReturn", XrefKind.METADATA,
                "Enclosing method descriptor");
        assertReference(result, "metadata/NestHost", XrefKind.METADATA, "Nest host");
        assertReference(result, "metadata/NestMember", XrefKind.METADATA, "Nest member");
        assertReference(
                result, "metadata/Permitted", XrefKind.METADATA, "Permitted subclass");
        assertReference(result, "metadata/Outer$Inner", XrefKind.METADATA,
                "Inner class metadata");
        assertReference(
                result, "metadata/Outer", XrefKind.METADATA, "Inner class owner");
        assertReference(result, "module/Main", XrefKind.METADATA, "Module main class");
        assertReference(
                result, "module/Service", XrefKind.METADATA, "Module service use");
        assertReference(result, "module/ProvidedService", XrefKind.METADATA,
                "Module service");
        assertReference(result, "module/Provider", XrefKind.METADATA,
                "Module service provider");
        assertReference(
                result, "record/Descriptor", XrefKind.METADATA, "Record component type");
        assertReference(result, "record/Generic", XrefKind.METADATA,
                "Record component signature");
        assertMember(result, "metadata/Outer", "create",
                "(Lmetadata/OuterArgument;)Lmetadata/OuterReturn;",
                XrefKind.METADATA, XrefAccessType.READ, "Enclosing method");
        assertMember(result, "annotation/Enum", "ENTRY", "Lannotation/Enum;",
                XrefKind.ANNOTATION, XrefAccessType.READ);
    }

    @Test
    void extractsInstructionDescriptorsFramesHandlesAndBootstrapTrees() {
        ClassNode node = baseClass();
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "instruction/New"));
        method.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "instruction/Array"));
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "[[Linstruction/Cast;"));
        method.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, "instruction/InstanceOf"));
        method.instructions.add(new MultiANewArrayInsnNode("[[Linstruction/MultiArray;", 2));
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETFIELD, "instruction/FieldOwner", "field",
                "Linstruction/FieldValue;"));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, "instruction/MethodOwner", "call",
                "(Linstruction/MethodArgument;)Linstruction/MethodReturn;", false));
        method.instructions.add(new LdcInsnNode(Type.getMethodType(
                "(Lliteral/MethodArgument;)Lliteral/MethodReturn;")));
        method.instructions.add(new LdcInsnNode(FIELD_HANDLE));

        ConstantDynamic nested = new ConstantDynamic(
                "nested", "Lcondy/NestedType;", BOOTSTRAP,
                FIELD_HANDLE, Type.getType("Lcondy/NestedArgument;"));
        ConstantDynamic outer = new ConstantDynamic(
                "outer", "Lcondy/OuterType;", BOOTSTRAP, nested);
        method.instructions.add(new LdcInsnNode(outer));
        method.instructions.add(new InvokeDynamicInsnNode(
                "dynamic",
                "(Lindy/Argument;)Lindy/Return;",
                BOOTSTRAP,
                Type.getType("Lindy/BootstrapType;"), nested));
        method.instructions.add(new FrameNode(
                Opcodes.F_FULL, 1, new Object[]{"frame/Local"}, 1,
                new Object[]{"[Lframe/Stack;"}));
        node.methods.add(method);

        AsmReferenceScanner.ScanResult result = AsmReferenceScanner.scanClass(node);
        Set<String> owners = owners(result);

        assertContainsAll(owners,
                "instruction/New", "instruction/Array", "instruction/Cast",
                "instruction/InstanceOf", "instruction/MultiArray",
                "instruction/FieldOwner", "instruction/FieldValue",
                "instruction/MethodOwner", "instruction/MethodArgument",
                "instruction/MethodReturn", "literal/MethodArgument",
                "literal/MethodReturn", "handle/Owner", "handle/Value",
                "condy/NestedType", "condy/NestedArgument", "condy/OuterType",
                "bootstrap/Owner", "bootstrap/Argument", "bootstrap/Return",
                "indy/Argument", "indy/Return", "indy/BootstrapType",
                "frame/Local", "frame/Stack");
        assertReference(result, "instruction/New", XrefKind.TYPE, "New");
        assertReference(result, "instruction/Array", XrefKind.TYPE, "New (Array)");
        assertReference(result, "instruction/Cast", XrefKind.TYPE, "Cast");
        assertReference(result, "instruction/InstanceOf", XrefKind.TYPE, "Instance Of");
        assertReference(
                result, "instruction/MultiArray", XrefKind.TYPE, "New (Multi Array)");
        assertReference(result, "instruction/FieldOwner", XrefKind.FIELD, "Field (Get)");
        assertReference(
                result, "instruction/FieldValue", XrefKind.DESCRIPTOR, "Field descriptor");
        assertReference(result, "instruction/MethodOwner", XrefKind.INVOKE, "Invoke (Virtual)");
        assertReference(result, "instruction/MethodArgument",
                XrefKind.DESCRIPTOR, "Invocation descriptor");
        assertReference(result, "instruction/MethodReturn",
                XrefKind.DESCRIPTOR, "Invocation descriptor");
        assertReference(result, "frame/Local", XrefKind.STACK_FRAME, "Stack frame");
        assertReference(result, "frame/Stack", XrefKind.STACK_FRAME, "Stack frame");
        assertMember(result, "instruction/FieldOwner", "field",
                "Linstruction/FieldValue;", XrefKind.FIELD, XrefAccessType.READ);
        assertMember(result, "instruction/MethodOwner", "call",
                "(Linstruction/MethodArgument;)Linstruction/MethodReturn;",
                XrefKind.INVOKE, XrefAccessType.EXECUTE);
        assertMember(result, "handle/Owner", "VALUE", "Lhandle/Value;",
                XrefKind.FIELD, XrefAccessType.READ);
        assertMember(result, "bootstrap/Owner", "bootstrap",
                "(Lbootstrap/Argument;)Lbootstrap/Return;",
                XrefKind.INVOKE, XrefAccessType.EXECUTE);
    }

    @Test
    void deduplicatesEquivalentReferencesAndIgnoresMalformedMetadata() {
        ClassNode node = baseClass();
        node.signature = "not a signature";
        FieldNode malformed = new FieldNode(
                Opcodes.ACC_PRIVATE, "bad", "not a descriptor", "also bad", null);
        node.fields.add(malformed);
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.instructions.add(new InvokeDynamicInsnNode(
                "dynamic", "()V", BOOTSTRAP,
                Type.getType("Lduplicate/Type;"), Type.getType("Lduplicate/Type;")));
        node.methods.add(method);

        AsmReferenceScanner.ScanResult result =
                assertDoesNotThrow(() -> AsmReferenceScanner.scanClass(node));
        long duplicates = result.classReferences().stream()
                .filter(reference -> reference.owner().equals("duplicate/Type"))
                .count();

        assertEquals(1, duplicates);
    }

    @Test
    void standaloneMethodScanMatchesTheMethodPortionOfAFullClassScan() {
        ClassNode node = baseClass();
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "run",
                "(Lrefresh/Argument;)[Lrefresh/Return;",
                "(Ljava/util/List<Lrefresh/Generic;>;)[Lrefresh/Return;",
                new String[]{"refresh/Exception"});
        method.instructions.add(new InvokeDynamicInsnNode(
                "dynamic", "()Lrefresh/Dynamic;", BOOTSTRAP,
                Type.getType("Lrefresh/BootstrapArgument;")));
        node.methods.add(method);

        AsmReferenceScanner.ScanResult full = AsmReferenceScanner.scanClass(node);
        AsmReferenceScanner.ScanResult incremental = AsmReferenceScanner.scanMethod(method);

        assertEquals(new HashSet<>(incremental.classReferences()),
                full.classReferences().stream()
                        .filter(reference -> reference.source().method() == method)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(new HashSet<>(incremental.memberReferences()),
                full.memberReferences().stream()
                        .filter(reference -> reference.source().method() == method)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "fixture/Owner";
        node.superName = "java/lang/Object";
        return node;
    }

    private static Set<String> owners(AsmReferenceScanner.ScanResult result) {
        Set<String> owners = new HashSet<>();
        result.classReferences().forEach(reference -> owners.add(reference.owner()));
        return owners;
    }

    private static void assertContainsAll(Set<String> actual, String... expected) {
        for (String owner : expected) {
            assertTrue(actual.contains(owner),
                    () -> "Missing " + owner + " from " + actual);
        }
    }

    private static void assertMember(AsmReferenceScanner.ScanResult result,
                                     String owner, String name, String descriptor,
                                     XrefKind kind, XrefAccessType access) {
        assertMember(result, owner, name, descriptor, kind, access, null);
    }

    private static void assertMember(AsmReferenceScanner.ScanResult result,
                                     String owner, String name, String descriptor,
                                     XrefKind kind, XrefAccessType access, String invocation) {
        assertTrue(result.memberReferences().stream().anyMatch(reference ->
                        reference.details().equals(new MemberDetails(owner, name, descriptor))
                                && reference.kind() == kind
                                && reference.access() == access
                                && (invocation == null
                                || reference.invocation().equals(invocation))),
                () -> "Missing member " + owner + '.' + name + descriptor);
    }

    private static void assertReference(AsmReferenceScanner.ScanResult result,
                                        String owner, XrefKind kind, String invocation) {
        assertTrue(result.classReferences().stream().anyMatch(reference ->
                        reference.owner().equals(owner)
                                && reference.kind() == kind
                                && reference.invocation().equals(invocation)),
                () -> "Missing " + kind + " reference to " + owner + " as " + invocation);
    }

    private static AnnotationNode annotation(String descriptor, Object value) {
        AnnotationNode annotation = new AnnotationNode(descriptor);
        annotation.values = values(value);
        return annotation;
    }

    private static TypeAnnotationNode typeAnnotation(int typeReference,
                                                     String descriptor, Object value) {
        TypeAnnotationNode annotation =
                new TypeAnnotationNode(typeReference, null, descriptor);
        annotation.values = values(value);
        return annotation;
    }

    private static List<Object> values(Object value) {
        return new ArrayList<>(List.of("value", value));
    }

    private static List<AnnotationNode>[] parameterAnnotations(AnnotationNode annotation) {
        @SuppressWarnings("unchecked")
        List<AnnotationNode>[] annotations = (List<AnnotationNode>[]) new List<?>[1];
        annotations[0] = List.of(annotation);
        return annotations;
    }
}
