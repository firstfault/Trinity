package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.components.AccessFlagsEditor;
import me.f1nal.trinity.gui.components.AnnotationEditor;
import me.f1nal.trinity.gui.components.general.LabeledTextList;
import me.f1nal.trinity.gui.frames.ClosableWindow;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

public class EditClassWindow extends ClosableWindow {
    private final ClassInput classInput;
    private final LabeledTextList detailsTextList = new LabeledTextList(true);
    private final AnnotationEditor visibleAnnotationsEditor;
    private final AnnotationEditor invisibleAnnotationsEditor;
    private final AccessFlagsEditor accessFlagsEditor;

    public EditClassWindow(ClassInput classInput, Trinity trinity) {
        super("Edit Class: " + classInput.getFullName(), 640, 500, trinity);
        this.classInput = classInput;
        this.detailsTextList.add("Class Name", classInput.getFullName());
        this.detailsTextList.add("Super Name", classInput.getSuperName());

        ClassNode node = classInput.getClassNode();
        this.visibleAnnotationsEditor = new AnnotationEditor(node.visibleAnnotations == null ? (node.visibleAnnotations = new ArrayList<>()) : node.visibleAnnotations, trinity);
        this.invisibleAnnotationsEditor = new AnnotationEditor(node.invisibleAnnotations == null ? (node.invisibleAnnotations = new ArrayList<>()) : node.invisibleAnnotations, trinity);
        this.accessFlagsEditor = new AccessFlagsEditor(classInput.getAccessFlags());
    }

    private void section(String label, @Nullable String tooltip) {
        ImGui.bulletText(label);
        if (tooltip != null) {
            ImGui.sameLine();
            ImGui.textDisabled("(?)");
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(tooltip);
            }
        }
    }

    private void section(String label) {
        section(label, null);
    }

    @Override
    protected void renderFrame() {
        section("Details");
        this.detailsTextList.draw();

        section("Runtime Annotations", "RetentionPolicy.RUNTIME");
        ImGui.sameLine();
        this.visibleAnnotationsEditor.draw();

        section("Class Annotations", "RetentionPolicy.CLASS");
        ImGui.sameLine();
        this.invisibleAnnotationsEditor.draw();

        section("Access Flags");
        this.accessFlagsEditor.draw();
    }
}
