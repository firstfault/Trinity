package me.f1nal.trinity.execution.patch;

import me.f1nal.trinity.execution.patch.classes.ClassPatch;
import me.f1nal.trinity.execution.patch.classes.impl.ClassPatchDoubleInterface;
import me.f1nal.trinity.execution.patch.classes.impl.ClassPatchEnumFieldOrder;
import me.f1nal.trinity.execution.patch.classes.impl.ClassPatchNullAnnotation;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class ClassPatchManager {
    private static final List<ClassPatch> classPatchList = List.of(
            new ClassPatchEnumFieldOrder(),
            new ClassPatchNullAnnotation(),
            new ClassPatchDoubleInterface()
    );

    public static List<ClassPatch> getClassPatchList() {
        return classPatchList;
    }

    public static void patchForDecompilation(ClassNode classNode, boolean treatEnumAsClass) {
        classPatchList.stream()
                .filter(patch -> !(treatEnumAsClass && patch instanceof ClassPatchEnumFieldOrder))
                .filter(patch -> patch.isEnabled(classNode))
                .forEach(patch -> patch.patch(classNode));
    }
}
