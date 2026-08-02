package me.f1nal.trinity.execution.constant;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsmConstantScannerTest {
    private static final Handle BOOTSTRAP = new Handle(
            Opcodes.H_INVOKESTATIC, "bootstrap/Owner", "make", "()V", false);

    @Test
    void scansEveryConstantBearingClassMemberAndInstructionLocation() {
        ClassNode node = baseClass();
        node.visibleAnnotations = List.of(annotation("class annotation"));

        RecordComponentNode record =
                new RecordComponentNode("record", "Ljava/lang/String;", null);
        record.visibleTypeAnnotations = List.of(typeAnnotation(11));
        node.recordComponents = List.of(record);

        FieldNode field = new FieldNode(
                Opcodes.ACC_STATIC, "field", "Ljava/lang/String;", null, "field value");
        field.visibleAnnotations = List.of(annotation(12));
        node.fields.add(field);

        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.annotationDefault = "annotation default";
        method.visibleAnnotations = List.of(annotation(13));
        method.visibleParameterAnnotations = parameterAnnotations(annotation(14));

        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        method.instructions.add(start);
        LdcInsnNode ldc = new LdcInsnNode("ldc value");
        ldc.visibleTypeAnnotations = List.of(typeAnnotation("instruction annotation"));
        method.instructions.add(ldc);
        ConstantDynamic dynamic = new ConstantDynamic(
                "dynamic", "Ljava/lang/String;", BOOTSTRAP, "condy argument");
        method.instructions.add(new LdcInsnNode(dynamic));
        method.instructions.add(new InvokeDynamicInsnNode(
                "call", "()V", BOOTSTRAP, "indy argument", "duplicate", "duplicate"));
        method.instructions.add(end);
        method.instructions.add(handler);

        LocalVariableAnnotationNode local = new LocalVariableAnnotationNode(
                TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue(),
                null, new LabelNode[]{start}, new LabelNode[]{end}, new int[]{1},
                "Lfixture/Annotation;");
        local.values = values("local annotation");
        method.visibleLocalVariableAnnotations = List.of(local);

        TryCatchBlockNode tryCatch = new TryCatchBlockNode(start, end, handler, null);
        tryCatch.visibleTypeAnnotations = List.of(typeAnnotation("try annotation"));
        method.tryCatchBlocks = List.of(tryCatch);
        node.methods.add(method);

        List<AsmConstantScanner.Occurrence> occurrences = AsmConstantScanner.scan(node);
        List<Object> values = occurrences.stream()
                .map(AsmConstantScanner.Occurrence::value).toList();

        assertTrue(values.containsAll(List.of(
                "class annotation", 11, "field value", 12,
                "annotation default", 13, 14, "local annotation",
                "try annotation", "instruction annotation", "ldc value",
                dynamic, BOOTSTRAP, "condy argument", "indy argument", "duplicate")));
        assertTrue(occurrences.stream().anyMatch(occurrence ->
                occurrence.value().equals("instruction annotation")
                        && occurrence.kind() == AsmConstantScanner.Kind.ANNOTATION
                        && occurrence.site().instruction() == ldc));
        assertTrue(occurrences.stream().anyMatch(occurrence ->
                occurrence.value().equals("field value")
                        && occurrence.kind() == AsmConstantScanner.Kind.LITERAL
                        && occurrence.site().field() == field));

        List<Integer> duplicateIndexes = occurrences.stream()
                .filter(occurrence -> occurrence.value().equals("duplicate"))
                .map(AsmConstantScanner.Occurrence::instructionOccurrence)
                .toList();
        assertEquals(List.of(0, 1), duplicateIndexes);
    }

    @Test
    void occurrenceStatisticsUseInstructionAnnotationConstantsToo() {
        ClassNode node = baseClass();
        node.visibleAnnotations = List.of(annotation("repeated"));
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        LdcInsnNode instruction = new LdcInsnNode("other");
        instruction.visibleTypeAnnotations = List.of(typeAnnotation("repeated"));
        method.instructions.add(instruction);
        node.methods.add(method);

        assertEquals(2, ConstantStatisticsCache.countOccurrences(node, "repeated"));
    }

    private static ClassNode baseClass() {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "fixture/Constants";
        node.superName = "java/lang/Object";
        return node;
    }

    private static AnnotationNode annotation(Object value) {
        AnnotationNode annotation = new AnnotationNode("Lfixture/Annotation;");
        annotation.values = values(value);
        return annotation;
    }

    private static TypeAnnotationNode typeAnnotation(Object value) {
        TypeAnnotationNode annotation = new TypeAnnotationNode(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0).getValue(),
                null, "Lfixture/Annotation;");
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
