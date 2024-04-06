package me.f1nal.trinity.decompiler.output.effect;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import imgui.ImGui;

import java.util.List;
import java.util.function.Supplier;

public class TooltipEffect extends TextComponentEffect {
    private final Supplier<List<ColoredString>> text;

    public TooltipEffect(Supplier<List<ColoredString>> text) {
        this.text = text;
    }

    public Supplier<List<ColoredString>> getText() {
        return text;
    }

    @Override
    public void handleHover() {
        ImGui.beginTooltip();
        ColoredString.drawText(this.text.get());
        ImGui.endTooltip();
    }

    @Override
    public PopupMenu openPopup() {
        return null;
    }
}
