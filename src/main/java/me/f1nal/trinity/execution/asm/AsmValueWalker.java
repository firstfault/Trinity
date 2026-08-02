package me.f1nal.trinity.execution.asm;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.AnnotationNode;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Recursively visits values stored in annotations and JVM constant structures.
 */
public final class AsmValueWalker {
    private AsmValueWalker() {
    }

    public static void walk(Object value, Consumer<Object> visitor) {
        walk(value, visitor, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void walk(Object value, Consumer<Object> visitor, Set<Object> recursionStack) {
        if (value == null) return;
        boolean container = isContainer(value);
        if (container && !recursionStack.add(value)) return;
        visitor.accept(value);
        if (!container) return;

        try {
            if (value instanceof AnnotationNode annotation) {
                if (annotation.values != null) {
                    for (int index = 1; index < annotation.values.size(); index += 2) {
                        walk(annotation.values.get(index), visitor, recursionStack);
                    }
                }
            } else if (value instanceof ConstantDynamic dynamic) {
                walk(dynamic.getBootstrapMethod(), visitor, recursionStack);
                for (int index = 0; index < dynamic.getBootstrapMethodArgumentCount(); index++) {
                    walk(dynamic.getBootstrapMethodArgument(index), visitor, recursionStack);
                }
            } else if (value instanceof List<?> values) {
                for (Object nested : values) walk(nested, visitor, recursionStack);
            } else {
                int length = Array.getLength(value);
                for (int index = 0; index < length; index++) {
                    walk(Array.get(value, index), visitor, recursionStack);
                }
            }
        } finally {
            recursionStack.remove(value);
        }
    }

    private static boolean isContainer(Object value) {
        return value instanceof AnnotationNode
                || value instanceof ConstantDynamic
                || value instanceof List<?>
                || value.getClass().isArray() && !(value instanceof String[]);
    }
}
