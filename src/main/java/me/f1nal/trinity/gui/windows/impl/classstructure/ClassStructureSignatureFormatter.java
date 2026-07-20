package me.f1nal.trinity.gui.windows.impl.classstructure;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.AbstractClassStructureNodeInput;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeClass;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeField;
import me.f1nal.trinity.gui.windows.impl.classstructure.nodes.ClassStructureNodeMethod;

import java.util.List;

/** Produces the same readable, colored member signature shown by Class Structure. */
public final class ClassStructureSignatureFormatter {
    private ClassStructureSignatureFormatter() {
    }

    public static List<ColoredString> format(Input<?> input) {
        AbstractClassStructureNodeInput<?> node = null;
        if (input instanceof ClassInput classInput) {
            node = new ClassStructureNodeClass(classInput);
        } else if (input instanceof MethodInput methodInput) {
            node = new ClassStructureNodeMethod(methodInput);
        } else if (input instanceof FieldInput fieldInput) {
            node = new ClassStructureNodeField(fieldInput);
        }
        return node == null ? List.of() : node.createReadableSignature();
    }
}
