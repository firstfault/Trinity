package me.f1nal.trinity.gui.windows.impl.themes;

import imgui.ImGui;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.api.PopupWindow;

import java.util.function.Consumer;

public class ThemeNamePopup extends PopupWindow {
    private final ImString string = new ImString("New Theme", 32);
    private final Consumer<String> finish;

    public ThemeNamePopup(Trinity trinity, Consumer<String> finish) {
        super("Create Theme", trinity);
        this.finish = finish;
    }

    @Override
    protected void renderFrame() {
        ImGui.inputText("Theme Name", this.string);
        boolean disabled = string.get().isEmpty() || Main.getThemeManager().getTheme(this.string.get()) != null;
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button("Done")) {
            this.finish.accept(this.string.get());
            this.close();
        }
        if (disabled) ImGui.endDisabled();
        ImGui.sameLine();
        if (ImGui.button("Close")) {
            this.close();
        }
    }
}
