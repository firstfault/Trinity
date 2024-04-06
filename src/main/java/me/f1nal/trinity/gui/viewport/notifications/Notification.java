package me.f1nal.trinity.gui.viewport.notifications;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.ImVec4;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.util.Stopwatch;

import java.util.List;

public class Notification {
    private final ICaption header;
    private final List<ColoredString> text;
    private Stopwatch stopwatch;
    private long expireTime = 9000L;
    private ImVec4 bounds;
    private final String id = ComponentId.getId(this.getClass());
    private final NotificationType type;

    public Notification(NotificationType type, ICaption header, List<ColoredString> text) {
        this.header = header;
        this.text = text;
        this.type = type;
    }

    public boolean setBounds(float height, ImGuiViewport viewport) {
        this.bounds = new ImVec4(viewport.getWorkPosX() + viewport.getWorkSizeX() - 10, viewport.getWorkPosY() + viewport.getWorkSizeY() - 10 - height, 20, 40);
        return this.stopwatch.hasPassed(this.expireTime);
    }

    public float render() {
        long difference = this.stopwatch.getDifference();
        float minDifference = this.expireTime / 2.F;
        float alpha = 1.F;
        if (difference > minDifference) {
            alpha = alpha - (difference - minDifference) / minDifference;
        }
        float oldAlpha = ImGui.getStyle().getAlpha();
        ImGui.getStyle().setAlpha(alpha);
        ImGui.setNextWindowPos(this.bounds.x, this.bounds.y, ImGuiCond.Always, 1.F, 1.F);
        ImGui.begin("###NotifWnd" + id, ImGuiWindowFlags.NoDocking | /*Tooltip, internal flag*/(1<<25) | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoNav | ImGuiWindowFlags.NoNavFocus);
        ImGui.textColored(this.type.getColor(), this.type.getIcon());
        ImGui.sameLine(0.F, 0.F);
        ImGui.text(" " + this.header.getCaption());
        ImGui.separator();
        boolean hoveringWindow = ImGui.isWindowHovered();
        if (hoveringWindow) this.stopwatch.reset();
        ColoredString.drawText(this.text);
        float sizeY = ImGui.getWindowSizeY();
        ImGui.end();
        ImGui.getStyle().setAlpha(oldAlpha);
        if (hoveringWindow && ImGui.isMouseClicked(0)) {
            return -1.F;
        }
        return sizeY;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public Stopwatch getStopwatch() {
        return stopwatch;
    }

    public void setStopwatch(Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
    }

    public ICaption getHeader() {
        return header;
    }

    public List<ColoredString> getText() {
        return text;
    }
}
