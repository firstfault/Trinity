package me.f1nal.trinity.execution.dependency;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedHashSet;
import java.util.Set;

/** Collects class names from descriptors, signatures, metadata, and code. */
public final class ClassDependencyScanner {
    private ClassDependencyScanner() {
    }

    public static Set<String> collect(ClassNode classNode) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Remapper collector = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName != null && !internalName.isBlank()) names.add(internalName);
                return internalName;
            }
        };
        // ClassRemapper only creates field/method visitors when its delegate
        // does. A ClassWriter provides a complete no-output traversal target,
        // ensuring instruction and annotation references are also mapped.
        classNode.accept(new ClassRemapper(new ClassWriter(0), collector));
        names.remove(classNode.name);
        return names;
    }
}
