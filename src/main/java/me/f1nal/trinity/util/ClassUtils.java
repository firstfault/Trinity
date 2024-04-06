package me.f1nal.trinity.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassUtils {
    public static MethodNode findMethod(ClassNode classNode, String name, String desc, boolean isStatic) {
        for (MethodNode method : classNode.methods) {
            if (isStatic == ((method.access & Opcodes.ACC_STATIC) != 0)) {
                if (method.name.equals(name) && method.desc.equals(desc)) {
                    return method;
                }
            }
        }
        return null;
    }
}
