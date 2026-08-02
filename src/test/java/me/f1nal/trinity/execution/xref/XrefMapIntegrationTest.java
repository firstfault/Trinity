package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.util.UnsafeUtil;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XrefMapIntegrationTest {
    @Test
    void indexesDeclarationAndBootstrapClassAndMemberReferences() throws Exception {
        Execution execution = emptyExecution();
        ClassNode node = classNode("fixture/Owner");
        FieldNode field = new FieldNode(
                Opcodes.ACC_PRIVATE, "target", "Ltarget/FieldType;", null, null);
        node.fields.add(field);
        Handle bootstrap = new Handle(
                Opcodes.H_INVOKESTATIC,
                "target/Bootstrap",
                "link",
                "(Ltarget/BootstrapArgument;)Ltarget/BootstrapReturn;",
                false);
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "make", "()[[Ltarget/ArrayReturn;", null, null);
        MethodInsnNode annotatedCall = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, "target/Receiver", "call", "()V", false);
        TypeAnnotationNode instructionAnnotation = new TypeAnnotationNode(
                TypeReference.newTypeArgumentReference(TypeReference.CAST, 0).getValue(),
                null, "Ltarget/InstructionAnnotation;");
        instructionAnnotation.values = new ArrayList<>(java.util.List.of(
                "value", new String[]{"Ltarget/AnnotationEnum;", "ENTRY"}));
        annotatedCall.visibleTypeAnnotations = java.util.List.of(instructionAnnotation);
        method.instructions.add(annotatedCall);
        method.instructions.add(new InvokeDynamicInsnNode(
                "dynamic", "(Ltarget/DynamicArgument;)Ltarget/DynamicReturn;",
                bootstrap));
        node.methods.add(method);
        ClassInput input = install(execution, node);
        XrefMap xrefs = new XrefMap(execution);

        xrefs.rebuild();

        assertFalse(xrefs.queryClassReferences("target/FieldType").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/ArrayReturn").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/DynamicArgument").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/DynamicReturn").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/Bootstrap").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/BootstrapArgument").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/BootstrapReturn").isEmpty());
        assertFalse(xrefs.queryMemberReferences(
                "target/Bootstrap", "link",
                "(Ltarget/BootstrapArgument;)Ltarget/BootstrapReturn;").isEmpty());
        assertTrue(xrefs.queryMemberReferences(
                        "target/AnnotationEnum", "ENTRY", "Ltarget/AnnotationEnum;")
                .stream().allMatch(reference ->
                        reference.getInvocation().equals("ENTRY")));
        assertTrue(xrefs.queryClassReferences("target/FieldType").stream().anyMatch(reference ->
                reference.getWhere().getInput() == input.getDeclaredField(
                        "target", "Ltarget/FieldType;")));
    }

    @Test
    void incrementalMethodRefreshRemovesStaleReferencesAndUsesFullScanner() throws Exception {
        Execution execution = emptyExecution();
        ClassNode node = classNode("fixture/Owner");
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "value", "()Ltarget/Before;", null, null);
        node.methods.add(method);
        ClassInput input = install(execution, node);
        MethodInput methodInput = input.getDeclaredMethod("value", "()Ltarget/Before;");
        XrefMap xrefs = new XrefMap(execution);
        xrefs.rebuild();

        assertFalse(xrefs.queryClassReferences("target/Before").isEmpty());

        method.desc = "()[Ltarget/After;";
        xrefs.refreshMethod(methodInput);

        assertTrue(xrefs.queryClassReferences("target/Before").isEmpty());
        assertFalse(xrefs.queryClassReferences("target/After").isEmpty());
    }

    private static Execution emptyExecution() throws Exception {
        Execution execution = (Execution) UnsafeUtil.getUnsafe().allocateInstance(Execution.class);
        setField(execution, "classTargetMap", new HashMap<String, ClassTarget>());
        setField(execution, "classInputList", new ArrayList<ClassInput>());
        return execution;
    }

    private static void setField(Execution execution, String name, Object value) throws Exception {
        Field field = Execution.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(execution, value);
    }

    private static ClassInput install(Execution execution, ClassNode node) {
        ClassTarget target = new ClassTarget(node.name, 0);
        ClassInput input = new ClassInput(execution, node, target);
        target.setInput(input);
        node.fields.forEach(field -> input.addInput(new FieldInput(field, input)));
        node.methods.forEach(method -> input.addInput(new MethodInput(method, input)));
        execution.addClassTarget(target);
        execution.getClassList().add(input);
        return input;
    }

    private static ClassNode classNode(String name) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }
}
