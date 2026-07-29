package me.f1nal.trinity.execution.dependency;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassDependencyScannerTest {
    @Test
    void collectsDependenciesFromMetadataDescriptorsAndInstructions() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "example/Subject",
                "Ldep/Generic<Ldep/TypeArgument;>;", "dep/Base", new String[]{"dep/Interface"});
        writer.visitField(Opcodes.ACC_PRIVATE, "field", "Ldep/Field;", null, null).visitEnd();
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "method",
                "(Ldep/Argument;)[Ldep/Return;", null, new String[]{"dep/Exception"});
        method.visitCode();
        method.visitTypeInsn(Opcodes.NEW, "dep/Constructed");
        method.visitInsn(Opcodes.POP);
        method.visitLdcInsn(Type.getObjectType("dep/Literal"));
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "dep/Owner", "call", "()V", false);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        ClassNode node = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(node, 0);

        Set<String> dependencies = ClassDependencyScanner.collect(node);

        for (String expected : Set.of("dep/Base", "dep/Interface", "dep/Generic", "dep/TypeArgument",
                "dep/Field", "dep/Argument", "dep/Return", "dep/Exception", "dep/Constructed",
                "dep/Literal", "dep/Owner")) {
            assertTrue(dependencies.contains(expected), () -> "Missing " + expected);
        }
    }
}
