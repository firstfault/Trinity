package me.f1nal.trinity.gui.windows.impl.bytecode;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.packages.Package;

public final class BytecodeEditorLauncher {
    private BytecodeEditorLauncher() {
    }

    public static void edit(Input<?> input) {
        if (input instanceof ClassInput classInput) {
            Main.getWindowManager().addClosableWindow(new ClassEditorWindow(Main.getTrinity(), classInput));
        } else if (input instanceof MethodInput methodInput) {
            Main.getWindowManager().addClosableWindow(new MethodEditorWindow(Main.getTrinity(), methodInput));
        } else if (input instanceof FieldInput fieldInput) {
            Main.getWindowManager().addClosableWindow(new FieldEditorWindow(Main.getTrinity(), fieldInput));
        } else {
            throw new IllegalArgumentException("Unsupported bytecode input: " + input.getClass().getName());
        }
    }

    public static void addMethod(ClassInput owner) {
        Main.getWindowManager().addClosableWindow(new MethodEditorWindow(Main.getTrinity(), owner));
    }

    public static void addField(ClassInput owner) {
        Main.getWindowManager().addClosableWindow(new FieldEditorWindow(Main.getTrinity(), owner));
    }

    public static void addClass(Package targetPackage) {
        Main.getWindowManager().addClosableWindow(new ClassEditorWindow(Main.getTrinity(), targetPackage));
    }
}
