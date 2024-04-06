package me.f1nal.trinity.appdata;

import imgui.ImGui;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.TimeUtil;

import java.util.Objects;

public class RecentDatabaseEntry {
    private final String name;
    private final String path;
    private final long lastOpened;

    public RecentDatabaseEntry(String name, String path, long lastOpened) {
        this.name = name;
        this.path = path;
        this.lastOpened = lastOpened;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getLastOpened() {
        return lastOpened;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecentDatabaseEntry that = (RecentDatabaseEntry) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public void setHoveringText() {
        ImGui.beginTooltip();
        ImGui.text("Last opened ");
        ImGui.sameLine(0, 0);
        ImGui.textColored(CodeColorScheme.STRING, TimeUtil.getFormattedTime(getLastOpened()));
        ImGui.textDisabled(getPath());
        ImGui.endTooltip();
    }
}
