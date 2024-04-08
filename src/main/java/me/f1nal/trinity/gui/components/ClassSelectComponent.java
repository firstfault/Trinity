package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.frames.impl.ClassPickerPopup;
import me.f1nal.trinity.util.GuiUtil;

import java.util.function.Predicate;

public class ClassSelectComponent {
    private final ImString className = new ImString(256);
    private final String id = ComponentId.getId(this.getClass());
    private final Trinity trinity;
    private ClassTarget classInput;
    private final Predicate<ClassTarget> validClassPredicate;

    public ClassSelectComponent(Trinity trinity, Predicate<ClassTarget> validClassPredicate) {
        this.trinity = trinity;
        this.validClassPredicate = validClassPredicate;
    }

    public ClassTarget getClassInput() {
        return classInput;
    }

    public String getClassName() {
        return className.get();
    }

    public void draw() {
        if (ImGui.inputTextWithHint("Class Name###ClassName" + id, "java/lang/Object", className)) {
            this.queryClassInput();
        }
        ImGui.sameLine();
        if (ImGui.smallButton("...")) {
            Main.getDisplayManager().addPopup(new ClassPickerPopup(this.trinity, validClassPredicate, classInput -> {
                className.set(classInput.getDisplayOrRealName());
                this.queryClassInput();
            }));
        }
        GuiUtil.tooltip("Open Class Picker");
    }

    private void queryClassInput() {
        this.classInput = trinity.getExecution().getClassTargetByDisplayName(this.className.get());
    }
}
