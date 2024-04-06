package me.f1nal.trinity.execution.patch.classes.impl;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.patch.classes.ClassPatch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes enum fields being out of order.
 */
public class ClassPatchEnumFieldOrder extends ClassPatch {
    @Override
    public void patch(ClassNode classNode) {
        List<FieldNode> enumFields = new ArrayList<>();
        List<FieldNode> normalFields = new ArrayList<>();

        for (FieldNode field : classNode.fields) {
            if ((field.access & Opcodes.ACC_ENUM) != 0) {
                enumFields.add(field);
                continue;
            }

            normalFields.add(field);
        }

        classNode.fields.clear();
        classNode.fields.addAll(enumFields);
        classNode.fields.addAll(normalFields);
    }

    @Override
    public boolean isEnabled(ClassNode classNode) {
        return (classNode.access & Opcodes.ACC_ENUM) != 0 && !Main.getPreferences().isDecompilerEnumClass();
    }
}
