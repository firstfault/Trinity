package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.INameable;

import java.util.List;

public enum MoveButtonEnum implements INameable {
    UP(FontAwesomeIcons.ArrowUp, "Move Up", 1),
    DOWN(FontAwesomeIcons.ArrowDown, "Move Down", -1);

    private final String icon;
    private final String name;
    private final int delta;

    MoveButtonEnum(String icon, String name, int delta) {
        this.icon = icon;
        this.name = name;
        this.delta = delta;
    }

    public <T> void draw(T selection, List<T> list) {
        draw(selection, list, this.getName());
    }

    public <T> void draw(T selection, List<T> list, String text) {
        final int indexOf = list.indexOf(selection);
        final int nextIndex = indexOf - this.getDelta();

        final boolean disabled = selection == null || nextIndex < 0 || nextIndex >= list.size();

        GuiUtil.disabledWidget(disabled, () -> {
            if (ImGui.button(text)) {
                list.remove(selection);
                list.add(nextIndex, selection);
            }
        });
    }

    public int getDelta() {
        return delta;
    }

    @Override
    public String getName() {
        return this.icon + " " + this.name;
    }
}