package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.INameable;

import java.util.List;

public enum NextButtonEnum implements INameable {
    BACK(FontAwesomeIcons.ArrowLeft, "Back", -1),
    NEXT(FontAwesomeIcons.ArrowRight, "Next", 1);

    private final String icon;
    private final String name;
    private final int delta;

    NextButtonEnum(String icon, String name, int delta) {
        this.icon = icon;
        this.name = name;
        this.delta = delta;
    }

    public <T> T draw(T selection, List<T> list) {
        final int indexOf = list.indexOf(selection);
        final int nextIndex = indexOf + this.getDelta();

        final boolean disabled = selection == null || nextIndex < 0 || nextIndex >= list.size();
        //noinspection unchecked
        final T[] selected = (T[]) new Object[]{selection};

        GuiUtil.disabledWidget(disabled, () -> {
            if (ImGui.button(this.getName())) {
                selected[0] = list.get(nextIndex);
            }
        });

        return selected[0];
    }

    public int getDelta() {
        return delta;
    }

    @Override
    public String getName() {
        return this.icon + " " + this.name;
    }
}