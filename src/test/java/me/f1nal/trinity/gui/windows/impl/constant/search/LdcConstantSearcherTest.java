package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LdcConstantSearcherTest {
    private static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC,
            "bootstrap/Factory",
            "make",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                    + "Ljava/lang/Class;)Ljava/lang/Object;",
            false);
    private static final Handle DIRECT_HANDLE = new Handle(
            Opcodes.H_INVOKEVIRTUAL,
            "sample/Receiver",
            "accept",
            "(Ljava/lang/String;)V",
            false);

    @Test
    void findsTypesAcrossAnnotationAndInstructionLocations() {
        List<ConstantViewCache> results = searchTypes(fixture());

        assertAnnotationResult(results, "Lannotation/Class;");
        assertAnnotationResult(results, "Lannotation/Record;");
        assertAnnotationResult(results, "Lannotation/Field;");
        assertAnnotationResult(results, "Lannotation/Default;");
        assertAnnotationResult(results, "Lannotation/Parameter;");
        assertAnnotationResult(results, "Lannotation/Local;");
        assertAnnotationResult(results, "Lannotation/TryCatch;");
        assertAnnotationResult(results, "Lannotation/Instruction;");
        assertInstructionResult(results, "Lliteral/Direct;");
        assertInstructionResult(results, "Lbootstrap/CondyArgument;");
        assertInstructionResult(results, "Lbootstrap/IndyArgument;");
    }

    @Test
    void findsDirectAndBootstrapHandles() {
        List<ConstantViewCache> results = searchHandles(fixture());

        assertInstructionResult(results, DIRECT_HANDLE.toString());
        assertInstructionResult(results, BOOTSTRAP.toString());
    }

    @Test
    void findsNestedConstantDynamicValues() {
        List<ConstantViewCache> results = searchDynamicConstants(fixture());

        assertInstructionResult(results, "outer");
        assertInstructionResult(results, "nested");
    }

    private static ClassInput fixture() {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "sample/Owner";
        node.superName = "java/lang/Object";
        node.visibleAnnotations = List.of(annotation("Lsample/Annotation;",
                Type.getType("Lannotation/Class;")));

        RecordComponentNode component =
                new RecordComponentNode("value", "Ljava/lang/String;", null);
        component.visibleAnnotations = List.of(annotation("Lsample/Annotation;",
                Type.getType("Lannotation/Record;")));
        node.recordComponents = new ArrayList<>(List.of(component));

        ClassTarget target = new ClassTarget(node.name, 0);
        ClassInput input = new ClassInput(null, node, target);
        target.setInput(input);

        FieldNode field = new FieldNode(
                Opcodes.ACC_PRIVATE, "field", "Ljava/lang/String;", null, null);
        field.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTypeReference(TypeReference.FIELD).getValue(),
                Type.getType("Lannotation/Field;")));
        node.fields.add(field);
        input.addInput(new FieldInput(field, input));

        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "run", "(Ljava/lang/String;)V", null, null);
        method.annotationDefault = Type.getType("Lannotation/Default;");
        method.visibleParameterAnnotations = parameterAnnotations(annotation(
                "Lsample/Annotation;", Type.getType("Lannotation/Parameter;")));

        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.add(start);

        LdcInsnNode directType = new LdcInsnNode(Type.getType("Lliteral/Direct;"));
        directType.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTypeReference(TypeReference.CAST).getValue(),
                Type.getType("Lannotation/Instruction;")));
        method.instructions.add(directType);
        method.instructions.add(new LdcInsnNode(DIRECT_HANDLE));

        ConstantDynamic nested = new ConstantDynamic(
                "nested", "Ljava/lang/String;", BOOTSTRAP,
                Type.getType("Lbootstrap/CondyArgument;"));
        ConstantDynamic outer = new ConstantDynamic(
                "outer", "Ljava/lang/Object;", BOOTSTRAP, nested);
        method.instructions.add(new LdcInsnNode(outer));
        method.instructions.add(new InvokeDynamicInsnNode(
                "call", "()V", BOOTSTRAP,
                Type.getType("Lbootstrap/IndyArgument;"), nested));
        method.instructions.add(end);
        method.instructions.add(handler);

        LocalVariableAnnotationNode local = new LocalVariableAnnotationNode(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(),
                null,
                new LabelNode[]{start},
                new LabelNode[]{end},
                new int[]{1},
                "Lsample/Annotation;");
        local.values = values(Type.getType("Lannotation/Local;"));
        method.visibleLocalVariableAnnotations = List.of(local);

        TryCatchBlockNode tryCatch = new TryCatchBlockNode(start, end, handler, null);
        tryCatch.visibleTypeAnnotations = List.of(typeAnnotation(
                TypeReference.newTryCatchReference(0).getValue(),
                Type.getType("Lannotation/TryCatch;")));
        method.tryCatchBlocks = new ArrayList<>(List.of(tryCatch));

        node.methods.add(method);
        input.addInput(new MethodInput(method, input));
        return input;
    }

    private static List<AnnotationNode>[] parameterAnnotations(AnnotationNode annotation) {
        @SuppressWarnings("unchecked")
        List<AnnotationNode>[] annotations = (List<AnnotationNode>[]) new List<?>[1];
        annotations[0] = List.of(annotation);
        return annotations;
    }

    private static AnnotationNode annotation(String descriptor, Object value) {
        AnnotationNode annotation = new AnnotationNode(descriptor);
        annotation.values = values(value);
        return annotation;
    }

    private static TypeAnnotationNode typeAnnotation(int typeReference, Object value) {
        TypeAnnotationNode annotation =
                new TypeAnnotationNode(typeReference, null, "Lsample/Annotation;");
        annotation.values = values(value);
        return annotation;
    }

    private static List<Object> values(Object value) {
        return new ArrayList<>(List.of("value", value));
    }

    private static List<ConstantViewCache> searchTypes(ClassInput input) {
        List<ConstantViewCache> results = new ArrayList<>();
        new LdcConstantSearcher<Type>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof Type;
            }

            @Override
            protected String convertConstantToText(Type value) {
                return value.getDescriptor();
            }
        }.populateClass(results, input);
        return results;
    }

    private static List<ConstantViewCache> searchHandles(ClassInput input) {
        List<ConstantViewCache> results = new ArrayList<>();
        new LdcConstantSearcher<Handle>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof Handle;
            }

            @Override
            protected String convertConstantToText(Handle value) {
                return value.toString();
            }
        }.populateClass(results, input);
        return results;
    }

    private static List<ConstantViewCache> searchDynamicConstants(ClassInput input) {
        List<ConstantViewCache> results = new ArrayList<>();
        new LdcConstantSearcher<ConstantDynamic>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof ConstantDynamic;
            }

            @Override
            protected String convertConstantToText(ConstantDynamic value) {
                return value.getName();
            }
        }.populateClass(results, input);
        return results;
    }

    private static void assertAnnotationResult(List<ConstantViewCache> results, String constant) {
        assertTrue(results.stream().anyMatch(result ->
                        result.getConstant().equals(constant)
                                && result.getKind() == XrefKind.ANNOTATION),
                () -> "Missing annotation constant " + constant + " from " + describe(results));
    }

    private static void assertInstructionResult(List<ConstantViewCache> results, String constant) {
        assertTrue(results.stream().anyMatch(result ->
                        result.getConstant().equals(constant)
                                && result.getKind() == XrefKind.LITERAL
                                && result.getWhere() instanceof XrefWhereMethodInsn),
                () -> "Missing instruction constant " + constant + " from " + describe(results));
    }

    private static List<String> describe(List<ConstantViewCache> results) {
        return results.stream().map(result ->
                result.getConstant() + " [" + result.getKind() + ", "
                        + result.getWhere().getClass().getSimpleName() + "]").toList();
    }
}
