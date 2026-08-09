package me.f1nal.trinity.gui.windows.impl.mcp;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.mcp.McpActivityEvent;
import me.f1nal.trinity.mcp.McpActivityLog;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Console-style window that surfaces MCP server activity: lifecycle events,
 * connected agents, and every tool invocation with its origin agent.
 */
public final class McpStatusWindow extends StaticWindow {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private static final int MAX_LINE_EVENTS = 512;
    private static final int SEPARATOR_ALPHA = 110;
    private static final int DETAIL_ALPHA = 175;
    private static final float GROUP_SPACING = 14.F;
    private static final float ITEM_SPACING = 5.F;

    private boolean autoScroll = true;
    private int lastRenderedRevision = -1;
    private boolean stuckToBottom = true;

    public McpStatusWindow(Trinity trinity) {
        super("MCP Status", 540.F, 340.F, trinity);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!Main.getWindowManager().isResettingWindows()) {
            Main.getPreferences().setMcpStatusVisible(visible);
        }
    }

    @Override
    protected void renderFrame() {
        McpActivityLog log = Main.getMcpActivityLog();
        drawStatusRow(log);
        drawToolbarRow(log);
        drawAgentsSummary(log);
        ImGui.separator();
        drawConsole(log);
    }

    private void drawStatusRow(McpActivityLog log) {
        boolean running = log != null && log.isServerRunning();
        int statusColor = running ? CodeColorScheme.NOTIFY_SUCCESS : CodeColorScheme.NOTIFY_WARN;
        String statusText = running ? "Running" : (log == null ? "Disabled" : "Stopped");

        ImDrawList drawList = ImGui.getWindowDrawList();
        ImVec2 cursor = ImGui.getCursorScreenPos();
        float rowHeight = ImGui.getTextLineHeight();
        float dotRadius = rowHeight * 0.32F;
        float dotCenterX = cursor.x + dotRadius + 2.F;
        float dotCenterY = cursor.y + rowHeight * 0.5F;
        if (running) {
            drawList.addCircleFilled(dotCenterX, dotCenterY, dotRadius, statusColor, 18);
        } else {
            drawList.addCircle(dotCenterX, dotCenterY, dotRadius, statusColor, 18, 2.F);
        }

        ImGui.dummy(dotRadius * 2.F + 8.F, rowHeight);
        ImGui.sameLine();
        ImGui.textColored(statusColor, statusText);

        ImGui.sameLine(0.F, GROUP_SPACING);
        ImGui.textDisabled("Endpoint");
        ImGui.sameLine(0.F, ITEM_SPACING);
        String endpoint = log == null ? "" : log.getEndpoint();
        if (endpoint.isEmpty()) endpoint = "-";
        ImGui.textColored(CodeColorScheme.DISABLED, endpoint);

        ImGui.sameLine(0.F, ITEM_SPACING);
        if (ImGui.button("Copy URL###CopyEndpoint")) {
            ImGui.setClipboardText(endpoint);
        }
        GuiUtil.tooltip("Copy MCP endpoint URL");
    }

    private void drawToolbarRow(McpActivityLog log) {
        drawStat("Calls", log == null ? 0 : log.getTotalToolCalls(), CodeColorScheme.TEXT);
        ImGui.sameLine(0.F, GROUP_SPACING);
        int failed = log == null ? 0 : log.getFailedToolCalls();
        drawStat("Failed", failed, failed == 0 ? CodeColorScheme.DISABLED : CodeColorScheme.NOTIFY_ERROR);
        ImGui.sameLine(0.F, GROUP_SPACING);
        drawStat("Agents", log == null ? 0 : log.snapshotAgents().size(), CodeColorScheme.PARAM_REF);

        ImGui.sameLine(0.F, GROUP_SPACING);
        if (ImGui.button("Clear###ClearMcpLog")) {
            if (log != null) log.clear();
            lastRenderedRevision = -1;
        }

        ImGui.sameLine(0.F, ITEM_SPACING);
        if (ImGui.checkbox("Auto-scroll", autoScroll)) {
            autoScroll = !autoScroll;
        }
        GuiUtil.tooltip("Keep the console pinned to the latest event.");
    }

    private static void drawStat(String label, int value, int valueColor) {
        ImGui.textDisabled(label);
        ImGui.sameLine(0.F, ITEM_SPACING);
        ImGui.textColored(valueColor, String.valueOf(value));
    }

    private void drawAgentsSummary(McpActivityLog log) {
        List<McpActivityLog.AgentSnapshot> agents = log == null ? List.of() : log.snapshotAgents();
        if (agents.isEmpty()) return;
        ColoredStringBuilder csb = ColoredStringBuilder.create();
        csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, DETAIL_ALPHA), "Connected: ");
        for (int i = 0; i < agents.size(); i++) {
            McpActivityLog.AgentSnapshot agent = agents.get(i);
            if (i > 0) {
                csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, SEPARATOR_ALPHA), "  |  ");
            }
            csb.text(CodeColorScheme.NOTIFY_SUCCESS, "* ");
            csb.text(CodeColorScheme.PARAM_REF, agent.name().isEmpty() ? "agent" : agent.name());
            if (!agent.version().isEmpty()) {
                csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, DETAIL_ALPHA),
                        " v" + agent.version());
            }
        }
        ColoredString.drawText(csb.get());
    }

    private void drawConsole(McpActivityLog log) {
        float footerHeight = ImGui.getFrameHeightWithSpacing();
        if (ImGui.beginChild(getId("McpConsole"), 0.F, -footerHeight, false,
                ImGuiWindowFlags.AlwaysVerticalScrollbar)) {
            float prevScrollY = ImGui.getScrollY();
            float prevScrollMax = ImGui.getScrollMaxY();
            boolean wasAtBottom = prevScrollMax <= 0.F || prevScrollY >= prevScrollMax - 4.F;
            if (wasAtBottom) stuckToBottom = true;
            if (prevScrollMax > 0.F && prevScrollY < prevScrollMax - 8.F) stuckToBottom = false;

            List<McpActivityEvent> events = log == null ? List.of() : log.snapshotEvents();
            List<LineRecord> lines = new ArrayList<>();
            if (events.isEmpty()) {
                ImGui.textDisabled("No MCP activity yet.");
            } else {
                int from = Math.max(0, events.size() - MAX_LINE_EVENTS);
                for (int i = from; i < events.size(); i++) {
                    McpActivityEvent event = events.get(i);
                    float topY = ImGui.getCursorScreenPos().y;
                    ColoredString.drawText(formatEvent(event));
                    float bottomY = ImGui.getCursorScreenPos().y;
                    lines.add(new LineRecord(toPlainText(event), topY, bottomY));
                }
            }

            handleRightClick(log, lines);

            int revision = log == null ? 0 : log.getRevision();
            boolean newContent = lastRenderedRevision != revision;
            if (newContent && autoScroll && stuckToBottom) {
                ImGui.setScrollHereY(1.F);
            }
            lastRenderedRevision = revision;
        }
        ImGui.endChild();

        drawConsoleFooter(log);
    }

    private void handleRightClick(McpActivityLog log, List<LineRecord> lines) {
        if (!ImGui.isWindowHovered()) return;
        if (!ImGui.isMouseClicked(1)) return;

        ImVec2 mouse = ImGui.getMousePos();
        String hitLine = null;
        for (LineRecord line : lines) {
            if (mouse.y >= line.topY && mouse.y <= line.bottomY) {
                hitLine = line.text;
                break;
            }
        }

        PopupItemBuilder popup = PopupItemBuilder.create();
        final String lineText = hitLine;
        if (lineText != null) {
            popup.menuItem("Copy Line", () -> ImGui.setClipboardText(lineText));
        } else {
            popup.menuItem("Copy Line", "", false, () -> {});
        }
        String allText = buildAllText(log);
        popup.menuItem("Copy All", allText.isEmpty(), () -> ImGui.setClipboardText(allText));
        popup.separator();
        popup.menuItem("Clear Log", log == null || log.snapshotEvents().isEmpty(),
                () -> { if (log != null) log.clear(); });
        Main.getDisplayManager().getPopupMenu().show(popup);
    }

    private static String buildAllText(McpActivityLog log) {
        if (log == null) return "";
        StringBuilder sb = new StringBuilder();
        for (McpActivityEvent event : log.snapshotEvents()) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(toPlainText(event));
        }
        return sb.toString();
    }

    private void drawConsoleFooter(McpActivityLog log) {
        int total = log == null ? 0 : log.snapshotEvents().size();
        int shown = Math.min(total, MAX_LINE_EVENTS);
        ImGui.textDisabled("Showing");
        ImGui.sameLine(0.F, ITEM_SPACING);
        ImGui.textColored(CodeColorScheme.TEXT, shown + "/" + total);
        ImGui.sameLine(0.F, GROUP_SPACING);
        ImGui.textDisabled("Latest");
        ImGui.sameLine(0.F, ITEM_SPACING);
        long latest = log == null ? 0 : log.getLastEventMillis();
        ImGui.textColored(CodeColorScheme.DISABLED,
                latest == 0 ? "-" : TIME_FORMAT.format(Instant.ofEpochMilli(latest)
                        .atZone(ZoneId.systemDefault())));
        ImGui.sameLine(0.F, GROUP_SPACING);
        ImGui.textDisabled("Right-click a line to copy");
    }

    private static List<ColoredString> formatEvent(McpActivityEvent event) {
        ColoredStringBuilder csb = ColoredStringBuilder.create();
        String time = TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimestamp())
                .atZone(ZoneId.systemDefault()));
        csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, DETAIL_ALPHA), time);
        csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, SEPARATOR_ALPHA), "  ");

        LevelStyle style = LevelStyle.of(event);
        csb.text(style.color, style.tag);
        csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, SEPARATOR_ALPHA), "  ");

        String agent = event.getAgentName().isEmpty() ? "server" : event.getAgentName();
        csb.text(CodeColorScheme.VAR_REF, agent);
        csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, SEPARATOR_ALPHA), " > ");

        appendMessage(csb, event, style);
        return csb.get();
    }

    private static String toPlainText(McpActivityEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(TIME_FORMAT.format(Instant.ofEpochMilli(event.getTimestamp())
                .atZone(ZoneId.systemDefault()))).append("  ");
        sb.append(LevelStyle.of(event).tag.strip()).append("  ");
        String agent = event.getAgentName().isEmpty() ? "server" : event.getAgentName();
        sb.append(agent).append(" > ");
        switch (event.getType()) {
            case SERVER_STARTED, SERVER_STOPPED, SERVER_FAILED,
                 AGENT_CONNECTED, AGENT_DISCONNECTED -> {
                sb.append(event.getMessage());
                if (!event.getDetail().isEmpty()) sb.append("  ").append(event.getDetail());
            }
            case TOOL_CALLED -> {
                sb.append("called ").append(event.getToolName());
                if (!event.getDetail().isEmpty()) sb.append("(").append(event.getDetail()).append(")");
            }
            case TOOL_RESULT -> {
                sb.append(event.getMessage());
                if (!event.getDetail().isEmpty()) sb.append("  ").append(event.getDetail());
            }
        }
        return sb.toString();
    }

    private static void appendMessage(ColoredStringBuilder csb, McpActivityEvent event,
                                      LevelStyle style) {
        switch (event.getType()) {
            case SERVER_STARTED, SERVER_STOPPED, SERVER_FAILED,
                 AGENT_CONNECTED, AGENT_DISCONNECTED -> {
                csb.text(style.color, event.getMessage());
                if (!event.getDetail().isEmpty()) {
                    csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, DETAIL_ALPHA),
                            "  " + event.getDetail());
                }
            }
            case TOOL_CALLED -> {
                csb.text(CodeColorScheme.TEXT, "called ");
                csb.text(CodeColorScheme.METHOD_REF, event.getToolName());
                if (!event.getDetail().isEmpty()) {
                    csb.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 200),
                            "(" + event.getDetail() + ")");
                }
            }
            case TOOL_RESULT -> {
                csb.text(style.color, event.getMessage());
                if (!event.getDetail().isEmpty()) {
                    csb.text(CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_ERROR, 220),
                            "  " + truncate(event.getDetail(), 140));
                }
            }
        }
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    private record LineRecord(String text, float topY, float bottomY) { }

    private record LevelStyle(String tag, int color) {
        static LevelStyle of(McpActivityEvent event) {
            return switch (event.getType()) {
                case SERVER_STARTED -> new LevelStyle("START", CodeColorScheme.NOTIFY_SUCCESS);
                case SERVER_STOPPED -> new LevelStyle("STOP ", CodeColorScheme.NOTIFY_WARN);
                case SERVER_FAILED -> new LevelStyle("FAIL ", CodeColorScheme.NOTIFY_ERROR);
                case AGENT_CONNECTED -> new LevelStyle("CONN ", CodeColorScheme.NOTIFY_SUCCESS);
                case AGENT_DISCONNECTED -> new LevelStyle("DROP ", CodeColorScheme.NOTIFY_WARN);
                case TOOL_CALLED -> new LevelStyle("CALL ", CodeColorScheme.NOTIFY_INFORMATION);
                case TOOL_RESULT -> event.isSuccess()
                        ? new LevelStyle(" OK  ", CodeColorScheme.NOTIFY_SUCCESS)
                        : new LevelStyle("ERR  ", CodeColorScheme.NOTIFY_ERROR);
            };
        }
    }
}
