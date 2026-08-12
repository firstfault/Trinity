package me.f1nal.trinity.refactor.identity;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
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
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityClassTransformerTest {
    private static final String OLD = "sample/Old";
    private static final String RENAMED = "renamed/NewType";

    @Test
    void classRenameCoversEveryStandardAsmReferenceFamily() {
        ClassNode declaration = classNode(OLD, "java/lang/Object");
        ClassNode use = referenceDenseClass();
        IdentityMapping mapping = new IdentityMapping(
                Map.of(OLD, RENAMED), Set.of(), Set.of(), Set.of(), RENAMED);

        transform(mapping, List.of(declaration, use), use);

        String asmText = print(use);
        assertFalse(asmText.contains(OLD), () -> "unmapped class reference:\n" + asmText);
        assertTrue(asmText.contains(RENAMED));
        assertEquals(RENAMED, use.superName);
        assertEquals(RENAMED, use.module.mainClass);
        assertEquals("L" + RENAMED + ";", use.recordComponents.get(0).descriptor);
        assertEquals("([[L" + RENAMED + ";)L" + RENAMED + ";",
                use.methods.get(0).desc);
    }

    @Test
    void methodRenameCoversDeclarationsInheritedCallsHandlesAndLambdaSites() {
        ClassNode base = classNode("sample/Base", "java/lang/Object");
        base.methods.add(method(Opcodes.ACC_PUBLIC, "run", "()V"));
        ClassNode child = classNode("sample/Child", "sample/Base");
        child.methods.add(method(Opcodes.ACC_PUBLIC, "run", "()V"));
        ClassNode caller = classNode("sample/Caller", "java/lang/Object");
        MethodNode body = method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "call", "()V");
        body.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "sample/Child", "run", "()V", false));
        body.instructions.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKEVIRTUAL,
                "sample/Child", "run", "()V", false)));
        Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;", false);
        body.instructions.add(new InvokeDynamicInsnNode("run", "()Lsample/Base;", metafactory,
                Type.getMethodType("()V"),
                new Handle(Opcodes.H_INVOKEVIRTUAL, "sample/Child", "run", "()V", false),
                Type.getMethodType("()V")));
        caller.methods.add(body);

        Set<IdentityMemberKey> methods = new LinkedHashSet<>();
        methods.add(new IdentityMemberKey("sample/Base", "run", "()V"));
        methods.add(new IdentityMemberKey("sample/Child", "run", "()V"));
        IdentityMapping mapping = new IdentityMapping(
                Map.of(), methods, Set.of(), Set.of(), "execute");
        transform(mapping, List.of(base, child, caller), base, child, caller);

        assertEquals("execute", base.methods.get(0).name);
        assertEquals("execute", child.methods.get(0).name);
        assertEquals("execute", ((MethodInsnNode) body.instructions.get(0)).name);
        assertEquals("execute", ((Handle) ((LdcInsnNode) body.instructions.get(1)).cst).getName());
        InvokeDynamicInsnNode dynamic = (InvokeDynamicInsnNode) body.instructions.get(2);
        assertEquals("execute", dynamic.name);
        assertEquals("execute", ((Handle) dynamic.bsmArgs[1]).getName());
    }

    @Test
    void fieldRenameCoversInheritedAccessesHandlesAndEnumAnnotationValues() {
        ClassNode base = classNode("sample/Base", "java/lang/Object");
        base.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "value", "I", null, null));
        ClassNode child = classNode("sample/Child", "sample/Base");
        ClassNode use = classNode("sample/Use", "java/lang/Object");
        MethodNode body = method(Opcodes.ACC_STATIC, "read", "()V");
        body.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                "sample/Child", "value", "I"));
        body.instructions.add(new LdcInsnNode(new Handle(Opcodes.H_GETFIELD,
                "sample/Child", "value", "I", false)));
        Handle getStaticFinal = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/ConstantBootstraps", "getStaticFinal",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/Object;", false);
        body.instructions.add(new LdcInsnNode(new ConstantDynamic(
                "value", "I", getStaticFinal, Type.getObjectType("sample/Base"))));
        use.methods.add(body);

        IdentityMemberKey field = new IdentityMemberKey("sample/Base", "value", "I");
        IdentityMapping mapping = new IdentityMapping(
                Map.of(), Set.of(), Set.of(field), Set.of(), "amount");
        transform(mapping, List.of(base, child, use), base, child, use);

        assertEquals("amount", base.fields.get(0).name);
        assertEquals("amount", ((FieldInsnNode) body.instructions.get(0)).name);
        assertEquals("amount", ((Handle) ((LdcInsnNode) body.instructions.get(1)).cst).getName());
        assertEquals("amount", ((ConstantDynamic)
                ((LdcInsnNode) body.instructions.get(2)).cst).getName());

        ClassNode enumNode = classNode("sample/Choice", "java/lang/Enum");
        enumNode.fields.add(new FieldNode(Opcodes.ACC_ENUM | Opcodes.ACC_PUBLIC
                | Opcodes.ACC_STATIC, "FIRST", "Lsample/Choice;", null, null));
        ClassNode annotated = classNode("sample/Annotated", "java/lang/Object");
        AnnotationNode annotation = new AnnotationNode("Lsample/Marker;");
        annotation.values = new ArrayList<>(List.of(
                "choice", new String[]{"Lsample/Choice;", "FIRST"}));
        annotated.visibleAnnotations = new ArrayList<>(List.of(annotation));
        MethodNode enumUse = method(Opcodes.ACC_STATIC, "choose", "()V");
        Handle enumConstant = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/ConstantBootstraps", "enumConstant",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/Class;)Ljava/lang/Enum;", false);
        enumUse.instructions.add(new LdcInsnNode(new ConstantDynamic(
                "FIRST", "Lsample/Choice;", enumConstant)));
        Handle enumSwitch = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/runtime/SwitchBootstraps", "enumSwitch",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)"
                        + "Ljava/lang/invoke/CallSite;", false);
        enumUse.instructions.add(new InvokeDynamicInsnNode(
                "enumSwitch", "(Lsample/Choice;I)I", enumSwitch, "FIRST", "OTHER"));
        annotated.methods.add(enumUse);
        IdentityMemberKey constant = new IdentityMemberKey(
                "sample/Choice", "FIRST", "Lsample/Choice;");
        IdentityMapping enumMapping = new IdentityMapping(
                Map.of(), Set.of(), Set.of(constant), Set.of(), "SECOND");
        transform(enumMapping, List.of(enumNode, annotated), enumNode, annotated);

        assertEquals("SECOND", enumNode.fields.get(0).name);
        assertEquals("SECOND", ((String[]) annotation.values.get(1))[1]);
        assertEquals("SECOND", ((ConstantDynamic)
                ((LdcInsnNode) enumUse.instructions.get(0)).cst).getName());
        InvokeDynamicInsnNode switchInstruction =
                (InvokeDynamicInsnNode) enumUse.instructions.get(1);
        assertEquals("SECOND", switchInstruction.bsmArgs[0]);
        assertEquals("OTHER", switchInstruction.bsmArgs[1]);
    }

    @Test
    void recordRenameUpdatesObjectMethodsBootstrapComponentNames() {
        ClassNode record = classNode("sample/Entry", "java/lang/Record");
        record.access |= Opcodes.ACC_RECORD;
        record.recordComponents = new ArrayList<>(List.of(
                new RecordComponentNode("first", "I", null),
                new RecordComponentNode("other", "Ljava/lang/String;", null)));
        record.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "first", "I", null, null));
        record.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "other", "Ljava/lang/String;", null, null));
        record.methods.add(method(Opcodes.ACC_PUBLIC, "first", "()I"));
        MethodNode generated = method(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;");
        Handle objectMethods = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/runtime/ObjectMethods", "bootstrap",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;"
                        + "[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;", false);
        generated.instructions.add(new InvokeDynamicInsnNode("toString",
                "(Lsample/Entry;)Ljava/lang/String;", objectMethods,
                Type.getObjectType("sample/Entry"), "first;other",
                new Handle(Opcodes.H_GETFIELD, "sample/Entry", "first", "I", false),
                new Handle(Opcodes.H_GETFIELD, "sample/Entry", "other",
                        "Ljava/lang/String;", false)));
        record.methods.add(generated);

        IdentityMemberKey component = new IdentityMemberKey(
                "sample/Entry", "first", "I");
        IdentityMemberKey accessor = new IdentityMemberKey(
                "sample/Entry", "first", "()I");
        IdentityMapping mapping = new IdentityMapping(Map.of(), Set.of(accessor),
                Set.of(component), Set.of(component), "primary");
        transform(mapping, List.of(record), record);

        assertEquals("primary", record.recordComponents.get(0).name);
        assertEquals("primary", record.fields.get(0).name);
        assertEquals("primary", record.methods.get(0).name);
        InvokeDynamicInsnNode dynamic =
                (InvokeDynamicInsnNode) generated.instructions.get(0);
        assertEquals("primary;other", dynamic.bsmArgs[1]);
        assertEquals("primary", ((Handle) dynamic.bsmArgs[2]).getName());
        assertEquals("other", ((Handle) dynamic.bsmArgs[3]).getName());
    }

    @Test
    void annotationMethodRenameUpdatesElementNamesAndNestedValues() {
        ClassNode annotationType = classNode("sample/Marker", "java/lang/Object");
        annotationType.access |= Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE;
        annotationType.methods.add(method(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "value", "()Ljava/lang/Class;"));
        ClassNode use = classNode("sample/Use", "java/lang/Object");
        AnnotationNode nested = new AnnotationNode("Lsample/Marker;");
        nested.values = new ArrayList<>(List.of("value", Type.getObjectType(OLD)));
        AnnotationNode root = new AnnotationNode("Lsample/Marker;");
        root.values = new ArrayList<>(List.of("value", Type.getObjectType(OLD),
                "nested", nested));
        use.visibleAnnotations = new ArrayList<>(List.of(root));

        IdentityMemberKey element = new IdentityMemberKey(
                "sample/Marker", "value", "()Ljava/lang/Class;");
        IdentityMapping mapping = new IdentityMapping(
                Map.of(OLD, RENAMED), Set.of(element), Set.of(), Set.of(), "type");
        transform(mapping, List.of(annotationType, use), annotationType, use);

        assertEquals("type", annotationType.methods.get(0).name);
        assertEquals("type", root.values.get(0));
        assertEquals(Type.getObjectType(RENAMED), root.values.get(1));
        assertEquals("type", nested.values.get(0));
        assertEquals(Type.getObjectType(RENAMED), nested.values.get(1));
    }

    @Test
    void inverseMappingRestoresClassAndMemberReferences() {
        ClassNode base = classNode(OLD, "java/lang/Object");
        base.methods.add(method(Opcodes.ACC_PUBLIC, "run", "(Lsample/Old;)Lsample/Old;"));
        ClassNode use = classNode("sample/Use", "java/lang/Object");
        MethodNode body = method(Opcodes.ACC_STATIC, "call", "(Lsample/Old;)V");
        body.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, OLD, "run",
                "(Lsample/Old;)Lsample/Old;", false));
        use.methods.add(body);
        String beforeBase = print(base);
        String beforeUse = print(use);

        IdentityMemberKey method = new IdentityMemberKey(
                OLD, "run", "(Lsample/Old;)Lsample/Old;");
        IdentityMapping mapping = new IdentityMapping(
                Map.of(OLD, RENAMED), Set.of(method), Set.of(), Set.of(), "execute");
        transform(mapping, List.of(base, use), base, use);
        IdentityMapping inverse = mapping.inverse("run");
        transform(inverse, List.of(base, use), base, use);

        assertEquals(beforeBase, print(base));
        assertEquals(beforeUse, print(use));
    }

    private static ClassNode referenceDenseClass() {
        ClassNode node = classNode("sample/Use", OLD);
        node.signature = "L" + OLD + ";";
        node.interfaces = new ArrayList<>(List.of(OLD));
        node.outerClass = OLD;
        node.outerMethod = "factory";
        node.outerMethodDesc = "()L" + OLD + ";";
        node.nestHostClass = OLD;
        node.nestMembers = new ArrayList<>(List.of(OLD));
        node.permittedSubclasses = new ArrayList<>(List.of(OLD));
        node.innerClasses.add(new InnerClassNode(OLD, OLD, "Old", Opcodes.ACC_PUBLIC));

        node.module = new ModuleNode("sample.module", 0, null);
        node.module.mainClass = OLD;
        node.module.uses = new ArrayList<>(List.of(OLD));
        node.module.provides = new ArrayList<>(List.of(
                new ModuleProvideNode(OLD, new ArrayList<>(List.of(OLD)))));

        RecordComponentNode record = new RecordComponentNode(
                "component", "L" + OLD + ";", "L" + OLD + ";");
        record.visibleAnnotations = annotations(OLD);
        record.visibleTypeAnnotations = typeAnnotations(OLD);
        node.recordComponents = new ArrayList<>(List.of(record));

        FieldNode field = new FieldNode(Opcodes.ACC_PUBLIC, "field",
                "L" + OLD + ";", "L" + OLD + ";", Type.getObjectType(OLD));
        field.visibleAnnotations = annotations(OLD);
        field.visibleTypeAnnotations = typeAnnotations(OLD);
        node.fields.add(field);

        MethodNode method = method(Opcodes.ACC_PUBLIC, "method",
                "([[L" + OLD + ";)L" + OLD + ";");
        method.signature = "([[L" + OLD + ";)L" + OLD + ";";
        method.exceptions = new ArrayList<>(List.of(OLD));
        method.visibleAnnotations = annotations(OLD);
        method.visibleTypeAnnotations = typeAnnotations(OLD);
        @SuppressWarnings("unchecked")
        List<AnnotationNode>[] parameterAnnotations = new List[]{annotations(OLD)};
        method.visibleParameterAnnotations = parameterAnnotations;
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.add(start);
        TypeInsnNode typeInstruction = new TypeInsnNode(Opcodes.NEW, OLD);
        typeInstruction.visibleTypeAnnotations = typeAnnotations(OLD);
        method.instructions.add(typeInstruction);
        method.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, OLD));
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, OLD));
        method.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, OLD));
        method.instructions.add(new MultiANewArrayInsnNode("[[L" + OLD + ";", 2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                OLD, "work", "(L" + OLD + ";)L" + OLD + ";", false));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                OLD, "field", "L" + OLD + ";"));
        method.instructions.add(new LdcInsnNode(Type.getObjectType(OLD)));
        method.instructions.add(new LdcInsnNode(new Handle(Opcodes.H_INVOKEVIRTUAL,
                OLD, "work", "(L" + OLD + ";)V", false)));
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, OLD, "bootstrap",
                "()Ljava/lang/Object;", false);
        ConstantDynamic dynamic = new ConstantDynamic("constant", "L" + OLD + ";",
                bootstrap, Type.getObjectType(OLD), new Handle(Opcodes.H_GETFIELD,
                OLD, "field", "L" + OLD + ";", false));
        method.instructions.add(new LdcInsnNode(dynamic));
        method.instructions.add(new InvokeDynamicInsnNode("dynamic",
                "(L" + OLD + ";)L" + OLD + ";", bootstrap,
                Type.getObjectType(OLD), dynamic));
        method.instructions.add(new FrameNode(Opcodes.F_FULL, 1,
                new Object[]{OLD}, 1, new Object[]{"[L" + OLD + ";"}));
        method.instructions.add(end);
        method.instructions.add(handler);
        method.localVariables = new ArrayList<>(List.of(new LocalVariableNode(
                "local", "L" + OLD + ";", "L" + OLD + ";", start, end, 1)));
        method.tryCatchBlocks = new ArrayList<>(List.of(
                new TryCatchBlockNode(start, end, handler, OLD)));
        LocalVariableAnnotationNode localAnnotation = new LocalVariableAnnotationNode(
                0x40000000, TypePath.fromString(""), new LabelNode[]{start},
                new LabelNode[]{end}, new int[]{1}, "L" + OLD + ";");
        localAnnotation.values = new ArrayList<>(List.of("value", Type.getObjectType(OLD)));
        method.visibleLocalVariableAnnotations = new ArrayList<>(List.of(localAnnotation));
        node.methods.add(method);
        node.visibleAnnotations = annotations(OLD);
        node.visibleTypeAnnotations = typeAnnotations(OLD);
        return node;
    }

    private static void transform(IdentityMapping mapping, List<ClassNode> universeNodes,
                                  ClassNode... targets) {
        Map<String, IdentityRefactorSnapshot.SnapshotClass> project = new LinkedHashMap<>();
        for (ClassNode node : universeNodes) {
            project.put(node.name, new IdentityRefactorSnapshot.SnapshotClass(
                    null, IdentityRefactorSnapshot.cloneClass(node), 0L));
        }
        IdentityClassUniverse universe = new IdentityClassUniverse(project, Map.of());
        IdentityAsmRemapper remapper = new IdentityAsmRemapper(mapping, universe);
        for (ClassNode target : targets) {
            new IdentityClassTransformer(remapper, null).transform(target);
        }
    }

    private static ClassNode classNode(String name, String superName) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = superName;
        return node;
    }

    private static MethodNode method(int access, String name, String descriptor) {
        return new MethodNode(Opcodes.ASM9, access, name, descriptor, null, null);
    }

    private static ArrayList<AnnotationNode> annotations(String type) {
        AnnotationNode annotation = new AnnotationNode("L" + type + ";");
        annotation.values = new ArrayList<>(List.of("value", Type.getObjectType(type)));
        return new ArrayList<>(List.of(annotation));
    }

    private static ArrayList<TypeAnnotationNode> typeAnnotations(String type) {
        TypeAnnotationNode annotation = new TypeAnnotationNode(
                0x13000000, null, "L" + type + ";");
        annotation.values = new ArrayList<>(List.of("value", Type.getObjectType(type)));
        return new ArrayList<>(List.of(annotation));
    }

    private static String print(ClassNode node) {
        StringWriter output = new StringWriter();
        node.accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(output)));
        return output.toString();
    }
}
