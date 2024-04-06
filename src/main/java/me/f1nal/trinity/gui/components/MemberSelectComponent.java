package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.execution.MemberDetails;

public class MemberSelectComponent {
    private final ClassSelectComponent classSelectComponent;
    private final ImString name = new ImString(256);
    private final ImString descriptor = new ImString(256);
    private final String id = ComponentId.getId(this.getClass());

    public MemberSelectComponent(ClassSelectComponent classSelectComponent) {
        this.classSelectComponent = classSelectComponent;
    }

    public MemberDetails draw() {
        classSelectComponent.draw();
        ImGui.inputText("Name###Name" + id, name);
        ImGui.inputText("Descriptor###Desc" + id, descriptor);
        return new MemberDetails(classSelectComponent.getClassName(), name.get(), descriptor.get());
    }

    public ClassSelectComponent getClassSelectComponent() {
        return classSelectComponent;
    }
}
