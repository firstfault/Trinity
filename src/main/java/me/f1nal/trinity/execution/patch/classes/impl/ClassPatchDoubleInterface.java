package me.f1nal.trinity.execution.patch.classes.impl;

import me.f1nal.trinity.execution.patch.classes.ClassPatch;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

public class ClassPatchDoubleInterface extends ClassPatch {
    @Override
    public void patch(ClassNode classNode) {
        Set<String> interfaces = new LinkedHashSet<>(classNode.interfaces);
        classNode.interfaces.clear();
        classNode.interfaces.addAll(interfaces);
    }

    @Override
    public boolean isEnabled(ClassNode classNode) {
        return classNode.interfaces != null && classNode.interfaces.size() > 1;
    }
}
