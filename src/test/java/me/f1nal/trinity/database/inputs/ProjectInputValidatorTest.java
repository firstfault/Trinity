package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.ClassPath;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectInputValidatorTest {
    @Test
    void reportsDuplicateJvmNamesAcrossArchives() {
        ProjectInputSet inputs = new ProjectInputSet();
        inputs.addJar("one.jar", classPath("sample/Duplicate"));
        inputs.addJar("two.jar", classPath("sample/Duplicate"));

        assertEquals(1, ProjectInputValidator.validate(inputs).size());
    }

    private static ClassPath classPath(String className) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        writer.visitEnd();
        ClassPath path = new ClassPath();
        path.addClass(new UnreadClassBytes(className + ".class", writer.toByteArray()));
        return path;
    }
}
