package me.f1nal.trinity.gui.components.popup;

import imgui.ImGui;
import me.f1nal.trinity.gui.components.popup.items.PopupItem;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public class PopupMenuBar {
    private List<PopupItem> popupItems;
    private MenuBarProgress progress;

    public PopupMenuBar(PopupItemBuilder builder) {
        this.set(builder);
    }

    public void setProgress(MenuBarProgress progress) {
        this.progress = progress;
    }

    public void set(PopupItemBuilder builder) {
        this.popupItems = builder.get();
    }

    public void draw() {
        PopupMenu.style(true);
        ImGui.beginMenuBar();
        PopupMenuState state = new PopupMenuState();
        for (PopupItem popupItem : popupItems) {
            popupItem.draw(state);
        }
        if (progress != null) {
            ImGui.separator();

            ImGui.textColored(CodeColorScheme.TEXT, progress.getRoutineName() + ": ");
            ImGui.sameLine(0.F, 0.F);
            ImGui.textColored(CodeColorScheme.DISABLED, progress.getProgress() == -1 ? progress.getTaskName() : String.format("%s (%s%%)", progress.getTaskName(), progress.getProgress()));
        }
        ImGui.endMenuBar();
        PopupMenu.style(false);
    }
}
