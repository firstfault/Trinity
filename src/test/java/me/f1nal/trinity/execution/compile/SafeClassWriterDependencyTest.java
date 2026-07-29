package me.f1nal.trinity.execution.compile;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeClassWriterDependencyTest {
    @Test
    void dependencyHierarchyProducesVerifiableFrames() throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put("dep/Base", baseClass());
        classes.put("example/A", subclass("example/A"));
        classes.put("example/B", subclass("example/B"));
        byte[] unframedTarget = targetClass();

        Map<String, ClassNode> nodes = new HashMap<>();
        classes.forEach((name, bytes) -> nodes.put(name, readNode(bytes)));
        ClassNode objectNode = new ClassNode();
        objectNode.name = "java/lang/Object";
        nodes.put(objectNode.name, objectNode);
        ClassNode targetNode = readNode(unframedTarget);
        nodes.put(targetNode.name, targetNode);

        SafeClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, nodes::get, new Console());
        targetNode.accept(writer);
        classes.put("example/Target", writer.toByteArray());

        ByteArrayClassLoader loader = new ByteArrayClassLoader(classes);
        Class<?> target = loader.loadClass("example.Target");
        Method call = target.getMethod("call", boolean.class);

        assertEquals(7, call.invoke(null, true));
        assertEquals(7, call.invoke(null, false));
    }

    @Test
    void unresolvedHierarchyIsReportedWhileFallbackOutputIsStillProduced() {
        Map<String, ClassNode> nodes = new HashMap<>();
        nodes.put("example/A", readNode(subclass("example/A")));
        nodes.put("example/B", readNode(subclass("example/B")));
        ClassNode target = readNode(targetClass());
        Set<String> unresolved = new LinkedHashSet<>();

        SafeClassWriter writer = new SafeClassWriter(
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, nodes::get,
                new Console(), unresolved::add, false);
        target.accept(writer);

        assertTrue(writer.toByteArray().length > 0);
        assertTrue(unresolved.contains("dep/Base"));
    }

    private static byte[] baseClass() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "dep/Base", null, "java/lang/Object", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        MethodVisitor value = writer.visitMethod(Opcodes.ACC_PUBLIC, "value", "()I", null, null);
        value.visitCode();
        value.visitIntInsn(Opcodes.BIPUSH, 7);
        value.visitInsn(Opcodes.IRETURN);
        value.visitMaxs(0, 0);
        value.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] subclass(String name) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "dep/Base", null);
        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "dep/Base", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] targetClass() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "example/Target", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "call", "(Z)I", null, null);
        method.visitCode();
        Label useB = new Label();
        Label merge = new Label();
        method.visitVarInsn(Opcodes.ILOAD, 0);
        method.visitJumpInsn(Opcodes.IFEQ, useB);
        method.visitTypeInsn(Opcodes.NEW, "example/A");
        method.visitInsn(Opcodes.DUP);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "example/A", "<init>", "()V", false);
        method.visitJumpInsn(Opcodes.GOTO, merge);
        method.visitLabel(useB);
        method.visitTypeInsn(Opcodes.NEW, "example/B");
        method.visitInsn(Opcodes.DUP);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "example/B", "<init>", "()V", false);
        method.visitLabel(merge);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "dep/Base", "value", "()I", false);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static final class ByteArrayClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        private ByteArrayClassLoader(Map<String, byte[]> classes) {
            super(null);
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name.replace('.', '/'));
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
