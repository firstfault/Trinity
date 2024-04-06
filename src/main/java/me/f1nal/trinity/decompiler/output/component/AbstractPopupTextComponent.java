package me.f1nal.trinity.decompiler.output.component;

import imgui.ImGui;
import imgui.internal.flag.ImGuiItemFlags;

public abstract class AbstractPopupTextComponent extends AbstractTextComponent {
    public AbstractPopupTextComponent(String text) {
        super(text);
    }

    protected String getPopupId() {
        return "NUC".concat(String.valueOf(getId()));
    }

    @Override
    public boolean handleItemHover() {
        if (ImGui.isMouseClicked(1)) {
            ImGui.openPopup(getPopupId());
            return true;
        }
        return false;
    }

    @Override
    public void handleAfterDrawing() {
        super.handleAfterDrawing();
        if (ImGui.beginPopup(this.getPopupId(), ImGuiItemFlags.SelectableDontClosePopup)) {
            this.drawPopup();
        }
    }

    protected abstract void drawPopup();
}
