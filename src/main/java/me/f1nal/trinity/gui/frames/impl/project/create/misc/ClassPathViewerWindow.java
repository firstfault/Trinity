package me.f1nal.trinity.gui.frames.impl.project.create.misc;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.database.inputs.UnreadClassBytes;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.general.ListBoxComponent;
import me.f1nal.trinity.gui.frames.ClosableWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClassPathViewerWindow extends ClosableWindow {
    private final ClassPath classPath;
    private final ListBoxComponent<ClassPathViewerElement> listBoxComponent;

    public ClassPathViewerWindow(String fileName, ClassPath classPath) {
        super("Class Path Viewer: " + fileName, 600, 430, null);
        this.classPath = classPath;
        List<ClassPathViewerElement> elements = new ArrayList<>();
        for (UnreadClassBytes classBytes : classPath.getClasses()) {
            elements.add(new ClassPathViewerElement(classBytes.getEntryName(), classBytes.getBytes().length, () -> classPath.getClasses().remove(classBytes)));
        }
        classPath.getResources().forEach((name, bytes) -> {
            elements.add(new ClassPathViewerElement(name, bytes.length, () -> classPath.getResources().remove(name) != null));
        });
        this.listBoxComponent = new ListBoxComponent<>(elements);
        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize;
    }

    public ListBoxComponent<ClassPathViewerElement> getListBoxComponent() {
        return listBoxComponent;
    }

    @Override
    protected void renderFrame() {
        this.listBoxComponent.draw(150.F, 300.F);
        ImGui.sameLine();
        if (ImGui.beginChild(this.getId("ViewerChild"), 410.F, 300.F)) {
            ImGui.textWrapped(this.listBoxComponent.getSelection().getName());
            ImGui.textWrapped(this.listBoxComponent.getSelection().getDescription());
            ImGui.separator();

            if (ImGui.button(FontAwesomeIcons.Trash + " Remove")) {
                this.listBoxComponent.getSelection().remove();
                this.listBoxComponent.removeElement(this.listBoxComponent.getSelection());
            }
        }
        ImGui.endChild();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassPathViewerWindow that = (ClassPathViewerWindow) o;
        return Objects.equals(classPath, that.classPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classPath);
    }
}
