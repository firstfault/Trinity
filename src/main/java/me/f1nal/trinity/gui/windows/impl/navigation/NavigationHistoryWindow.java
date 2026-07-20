package me.f1nal.trinity.gui.windows.impl.navigation;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.navigation.NavigationEntry;
import me.f1nal.trinity.gui.navigation.NavigationHistory;
import me.f1nal.trinity.gui.windows.api.StaticWindow;

import java.util.List;

public final class NavigationHistoryWindow extends StaticWindow {
    public NavigationHistoryWindow(Trinity trinity) {
        super("Navigation History", 430.F, 300.F, trinity);
    }

    @Override
    protected void renderFrame() {
        NavigationHistory history = Main.getDisplayManager().getNavigationHistory();
        drawButton("Back (" + Main.getKeyBindManager().DECOMPILER_NAVIGATE_BACK.getKeyName() + ")",
                !history.canGoBack(), Main.getDisplayManager()::navigateBack);
        ImGui.sameLine();
        drawButton("Forward (" + Main.getKeyBindManager().DECOMPILER_NAVIGATE_FORWARD.getKeyName() + ")",
                !history.canGoForward(), Main.getDisplayManager()::navigateForward);
        ImGui.sameLine();
        if (ImGui.button("Clear")) history.clear();
        ImGui.separator();

        if (ImGui.beginChild(getId("NavigationEntries"), 0.F, 0.F)) {
            List<NavigationEntry> entries = history.getEntries();
            if (entries.isEmpty()) {
                ImGui.textDisabled("No navigation history yet.");
            }
            for (int i = 0; i < entries.size(); i++) {
                NavigationEntry entry = entries.get(i);
                if (ImGui.selectable(entry.describe(trinity) + "###NavigationEntry." + entry.id(),
                        i == history.getCurrentIndex())) {
                    Main.getDisplayManager().replayNavigation(i);
                }
            }
        }
        ImGui.endChild();
    }

    private static void drawButton(String label, boolean disabled, Runnable action) {
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button(label)) action.run();
        if (disabled) ImGui.endDisabled();
    }
}
