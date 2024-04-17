package me.f1nal.trinity.execution.patch.classes.impl;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.struct.gen.VarType;
import me.f1nal.trinity.execution.patch.classes.ClassPatch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes enum fields being out of order.
 */
public class ClassPatchNullAnnotation extends ClassPatch {
    @Override
    public void patch(ClassNode classNode) {
        parseAnnotations(classNode.invisibleAnnotations);
        parseAnnotations(classNode.visibleAnnotations);

        for (MethodNode method : classNode.methods) {
            parseAnnotations(method.invisibleAnnotations);
            parseAnnotations(method.visibleAnnotations);
            parseAnnotationsArray(method.invisibleParameterAnnotations);
            parseAnnotationsArray(method.visibleParameterAnnotations);
        }

        for (FieldNode field : classNode.fields) {
            parseAnnotations(field.invisibleAnnotations);
            parseAnnotations(field.visibleAnnotations);
        }
    }

    private void parseAnnotationsArray(List<AnnotationNode>[] array) {
        if (array == null) {
            return;
        }
        for (List<AnnotationNode> list : array) {
            this.parseAnnotations(list);
        }
    }

    private void parseAnnotations(List<AnnotationNode> list) {
        if (list == null) {
            return;
        }

        list.removeIf(node -> node.desc.isEmpty() || new VarType(node.desc).getValue() == null);
    }

    @Override
    public boolean isEnabled(ClassNode classNode) {
        return true;
    }
}
