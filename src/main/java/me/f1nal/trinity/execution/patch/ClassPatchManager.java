package me.f1nal.trinity.execution.patch;

import me.f1nal.trinity.execution.patch.classes.ClassPatch;
import me.f1nal.trinity.execution.patch.classes.impl.ClassPatchEnumFieldOrder;
import me.f1nal.trinity.execution.patch.classes.impl.ClassPatchNullAnnotation;

import java.util.List;

public class ClassPatchManager {
    private static final List<ClassPatch> classPatchList = List.of(new ClassPatchEnumFieldOrder(), new ClassPatchNullAnnotation());

    public static List<ClassPatch> getClassPatchList() {
        return classPatchList;
    }
}
