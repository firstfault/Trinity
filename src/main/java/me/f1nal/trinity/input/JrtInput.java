package me.f1nal.trinity.input;

import me.f1nal.trinity.logging.Logging;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class JrtInput {
    private final Map<String, ClassNode> jrtClasses = new HashMap<>();

    /**
     * Gets a Java runtime class by reading it from the current Java runtime or querying it from cache.
     * @param className Class name.
     * @return {@link ClassNode} representing a class from the Java runtime or {@code null} if such class doesn't exist.
     */
    public @Nullable ClassNode getClass(final String className) {
        final ClassNode classNode;
        if (!jrtClasses.containsKey(className)) {
            jrtClasses.put(className, classNode = readClass(className));
        } else {
            classNode = jrtClasses.get(className);
        }
        return classNode;
    }

    private @Nullable ClassNode readClass(String className) {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(className + ".class");
        if (resourceAsStream == null) {
            return null;
        }
        final ClassNode classNode;
        try {
            ClassReader classReader = new ClassReader(resourceAsStream);
            classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            Logging.error("Failed to process JRT class {}", className);
            return null;
        }
        try {
            resourceAsStream.close();
        } catch (IOException e) {
            Logging.error("Failed to close JRT stream for {}", className);
        }
        return classNode;
    }
}
