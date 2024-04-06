package me.f1nal.trinity.execution.patch.classes;

import org.objectweb.asm.tree.ClassNode;

/**
 * Patches for fixing obfuscated classes before decompilation.
 */
public abstract class ClassPatch {
    public abstract void patch(ClassNode classNode);
    public abstract boolean isEnabled(ClassNode classNode);
}
