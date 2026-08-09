package me.f1nal.trinity.gui.windows.impl.mcp;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.mcp.McpActivityEvent;
import me.f1nal.trinity.mcp.McpActivityLog;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Compact server controls and a readable stream of MCP activity. */
public final class McpStatusWindow extends StaticWindow {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private static final int MAX_LINE_EVENTS = 512;
    private static final int MUTED_ALPHA = 175;
    private static final int QUIET_ALPHA = 125;
    private static final int HOVER_ALPHA = 28;
    private static final float COMPACT_PADDING_X = 5.F;
    private static final float COMPACT_PADDING_Y = 1.F;
    private static final float COMPACT_ROUNDING = 1.F;
    private static final float COMPACT_VERTICAL_SPACING = 2.F;
    private static final float ROW_PADDING_Y = 1.F;
    private static final float ROW_MARKER_RADIUS = 2.5F;
    private static final float ROW_LEFT_PADDING = 6.F;

    private boolean autoScroll = true;
    private int lastRenderedRevision = -1;
    private boolean stuckToBottom = true;
    private boolean forceScrollToBottom;

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
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing,
                ImGui.getStyle().getItemSpacingX(), COMPACT_VERTICAL_SPACING);
        drawServerHeader(log);
        ImGui.separator();
        drawActivity(log);
        ImGui.popStyleVar();
    }

    private void drawServerHeader(McpActivityLog log) {
        boolean running = Main.isMcpServerRunning();
        String action = running ? "Stop" : "Start";

        pushCompactControls();
        float actionWidth = ImGui.calcTextSize(action).x + COMPACT_PADDING_X * 2.F
                + ImGui.getStyle().getCellPaddingX() * 2.F;
        int tableFlags = ImGuiTableFlags.SizingStretchProp
                | ImGuiTableFlags.NoSavedSettings | ImGuiTableFlags.NoPadOuterX;
        if (ImGui.beginTable(getId("McpServerHeader"), 2, tableFlags)) {
            ImGui.tableSetupColumn("Server", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("Action", ImGuiTableColumnFlags.WidthFixed, actionWidth);
            ImGui.tableNextRow();

            ImGui.tableNextColumn();
            drawServerIdentity(log, running);

            ImGui.tableNextColumn();
            if (ImGui.button(action + "###McpServerToggle")) {
                if (running) Main.stopMcpServer();
                else Main.startMcpServer();
            }
            GuiUtil.tooltip(running ? "Stop the local MCP server" : "Start the local MCP server");
            ImGui.endTable();
        }
        ImGui.popStyleVar(3);

        drawConnectedClients(log);
    }

    private void drawServerIdentity(McpActivityLog log, boolean running) {
        float rowHeight = ImGui.getFrameHeight();
        ImVec2 position = ImGui.getCursorScreenPos();
        int statusColor = running ? CodeColorScheme.NOTIFY_SUCCESS
                : CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA);
        ImGui.getWindowDrawList().addCircleFilled(
                position.x + ROW_MARKER_RADIUS,
                position.y + rowHeight * 0.5F,
                ROW_MARKER_RADIUS, statusColor, 12);

        ImGui.dummy(ROW_MARKER_RADIUS * 2.F + 4.F, rowHeight);
        ImGui.sameLine(0.F, 5.F);
        ImGui.alignTextToFramePadding();
        String endpoint = running && log != null ? log.getEndpoint() : "";
        String identity = endpoint.isEmpty() ? "MCP is off" : endpoint;
        ImGui.textColored(running ? CodeColorScheme.TEXT : CodeColorScheme.DISABLED, identity);

        if (running && !endpoint.isEmpty()) {
            ImGui.sameLine(0.F, 4.F);
            if (ImGui.button(FontAwesomeIcons.Copy + "###CopyMcpEndpoint")) {
                ImGui.setClipboardText(endpoint);
            }
            GuiUtil.tooltip("Copy endpoint");
        }
    }

    private void drawConnectedClients(McpActivityLog log) {
        List<McpActivityLog.AgentSnapshot> clients = log == null
                ? List.of() : log.snapshotAgents();
        if (clients.isEmpty()) return;

        ColoredStringBuilder text = ColoredStringBuilder.create()
                .text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                        FontAwesomeIcons.Users + "  ");
        for (int i = 0; i < clients.size(); i++) {
            McpActivityLog.AgentSnapshot client = clients.get(i);
            if (i > 0) {
                text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, QUIET_ALPHA), "  ·  ");
            }
            text.text(CodeColorScheme.PARAM_REF, displayClientName(client.name()));
            if (!client.version().isEmpty()) {
                text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                        " " + client.version());
            }
        }
        ColoredString.drawText(text.get());
    }

    private void drawActivity(McpActivityLog log) {
        float footerHeight = compactControlHeight()
                + ImGui.getStyle().getItemSpacingY() * 2.F
                + 2.F;
        if (ImGui.beginChild(getId("McpActivity"), 0.F, -footerHeight, false)) {
            float previousScrollY = ImGui.getScrollY();
            float previousScrollMax = ImGui.getScrollMaxY();
            boolean wasAtBottom = previousScrollMax <= 0.F
                    || previousScrollY >= previousScrollMax - 4.F;
            if (wasAtBottom) stuckToBottom = true;
            if (previousScrollMax > 0.F && previousScrollY < previousScrollMax - 8.F) {
                stuckToBottom = false;
            }

            List<McpActivityEvent> events = log == null ? List.of() : log.snapshotEvents();
            boolean openedRowContext = false;
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing,
                    ImGui.getStyle().getItemSpacingX(), 0.F);
            if (events.isEmpty()) {
                ImGui.setCursorPosX(ImGui.getCursorPosX() + ROW_LEFT_PADDING);
                ImGui.setCursorPosY(ImGui.getCursorPosY() + 2.F);
                ImGui.textDisabled("No activity");
            } else {
                int from = Math.max(0, events.size() - MAX_LINE_EVENTS);
                for (int i = from; i < events.size(); i++) {
                    openedRowContext |= drawEventRow(log, events.get(i), i);
                }
            }
            ImGui.popStyleVar();

            if (!openedRowContext && ImGui.isWindowHovered()
                    && ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
                showActivityMenu(log, null);
            }

            int revision = log == null ? 0 : log.getRevision();
            boolean newContent = lastRenderedRevision != revision;
            if (forceScrollToBottom || (newContent && autoScroll && stuckToBottom)) {
                ImGui.setScrollHereY(1.F);
                forceScrollToBottom = false;
            }
            lastRenderedRevision = revision;
        }
        ImGui.endChild();

        ImGui.separator();
        drawFooter(log);
    }

    private boolean drawEventRow(McpActivityLog log, McpActivityEvent event, int index) {
        EventPresentation presentation = presentation(event);
        float lineHeight = ImGui.getTextLineHeight();
        boolean hasDetail = !presentation.detail().isEmpty();
        float rowHeight = lineHeight + ROW_PADDING_Y * 2.F + (hasDetail ? lineHeight : 0.F);
        float width = Math.max(1.F, ImGui.getContentRegionAvailX());
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();

        ImGui.invisibleButton("###McpEvent." + event.getTimestamp() + "." + index, width, rowHeight);
        boolean hovered = ImGui.isItemHovered();
        ImDrawList drawList = ImGui.getWindowDrawList();
        if (hovered) {
            drawList.addRectFilled(x, y, x + width, y + rowHeight,
                    CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, HOVER_ALPHA));
        }

        float textY = y + ROW_PADDING_Y;
        float markerX = x + ROW_LEFT_PADDING + ROW_MARKER_RADIUS;
        float markerY = textY + lineHeight * 0.5F;
        drawList.addCircleFilled(markerX, markerY, ROW_MARKER_RADIUS,
                presentation.color(), 12);

        float timestampX = markerX + ROW_MARKER_RADIUS + 7.F;
        String timestamp = formatTime(event.getTimestamp());
        drawList.addText(timestampX, textY,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA), timestamp);

        float timestampWidth = ImGui.calcTextSize("00:00:00").x;
        float messageX = timestampX + timestampWidth + 13.F;
        ColoredString.drawText(drawList, messageX, textY, presentation.message());

        if (hasDetail) {
            float maximumWidth = Math.max(20.F, x + width - messageX - 6.F);
            String visibleDetail = ellipsize(presentation.detail(), maximumWidth);
            drawList.addText(messageX, textY + lineHeight,
                    CodeColorScheme.setAlpha(presentation.color(), MUTED_ALPHA), visibleDetail);
            if (hovered && !visibleDetail.equals(presentation.detail())) {
                GuiUtil.tooltip(presentation.detail());
            }
        }

        if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            showActivityMenu(log, toPlainText(event));
            return true;
        }
        return false;
    }

    private void drawFooter(McpActivityLog log) {
        int total = log == null ? 0 : log.snapshotEvents().size();
        int shown = Math.min(total, MAX_LINE_EVENTS);
        int calls = log == null ? 0 : log.getTotalToolCalls();
        int clients = log == null ? 0 : log.snapshotAgents().size();
        int failed = log == null ? 0 : log.getFailedToolCalls();

        StringBuilder summary = new StringBuilder();
        if (shown != total) summary.append(shown).append('/').append(total);
        else summary.append(total);
        summary.append(total == 1 ? " event" : " events");
        summary.append("  ·  ").append(calls).append(calls == 1 ? " call" : " calls");
        summary.append("  ·  ").append(clients).append(clients == 1 ? " client" : " clients");
        if (failed > 0) summary.append("  ·  ").append(failed).append(" failed");

        pushCompactControls();
        String followText = FontAwesomeIcons.Check + " Follow";
        float followWidth = ImGui.calcTextSize(followText).x + COMPACT_PADDING_X * 2.F;
        float clearWidth = ImGui.calcTextSize(FontAwesomeIcons.TrashAlt).x + COMPACT_PADDING_X * 2.F;
        float controlsWidth = followWidth + clearWidth + ImGui.getStyle().getItemSpacingX()
                + ImGui.getStyle().getCellPaddingX() * 2.F;
        int tableFlags = ImGuiTableFlags.SizingStretchProp
                | ImGuiTableFlags.NoSavedSettings | ImGuiTableFlags.NoPadOuterX;
        if (ImGui.beginTable(getId("McpActivityFooter"), 2, tableFlags)) {
            ImGui.tableSetupColumn("Summary", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("Controls", ImGuiTableColumnFlags.WidthFixed, controlsWidth);
            ImGui.tableNextRow();

            ImGui.tableNextColumn();
            ImGui.alignTextToFramePadding();
            ImGui.textColored(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                    summary.toString());

            ImGui.tableNextColumn();
            String followLabel = (autoScroll ? FontAwesomeIcons.Check + " " : "")
                    + "Follow###McpFollow";
            if (ImGui.button(followLabel, followWidth, 0.F)) {
                autoScroll = !autoScroll;
                if (autoScroll) {
                    stuckToBottom = true;
                    forceScrollToBottom = true;
                }
            }
            GuiUtil.tooltip(autoScroll
                    ? "Following new activity" : "Keep the current scroll position");

            ImGui.sameLine();
            if (total == 0) ImGui.beginDisabled();
            if (ImGui.button(FontAwesomeIcons.TrashAlt + "###ClearMcpActivity",
                    clearWidth, 0.F)) {
                log.clear();
                lastRenderedRevision = -1;
            }
            if (total == 0) ImGui.endDisabled();
            GuiUtil.tooltip("Clear activity");
            ImGui.endTable();
        }
        ImGui.popStyleVar(3);
    }

    private void showActivityMenu(McpActivityLog log, String eventText) {
        PopupItemBuilder popup = PopupItemBuilder.create();
        if (eventText == null) {
            popup.menuItem("Copy Event", "", false, () -> { });
        } else {
            popup.menuItem("Copy Event", () -> ImGui.setClipboardText(eventText));
        }
        String allText = buildAllText(log);
        popup.menuItem("Copy All", allText.isEmpty(), () -> ImGui.setClipboardText(allText));
        popup.separator();
        popup.menuItem("Clear Activity", log == null || log.snapshotEvents().isEmpty(),
                () -> { if (log != null) log.clear(); });
        Main.getDisplayManager().getPopupMenu().show(popup);
    }

    private static EventPresentation presentation(McpActivityEvent event) {
        ColoredStringBuilder text = ColoredStringBuilder.create();
        int color = eventColor(event);
        String detail = "";
        String client = displayClientName(event.getAgentName());

        switch (event.getType()) {
            case SERVER_STARTED -> {
                text.text(CodeColorScheme.TEXT, "Server started");
                String endpoint = extractEndpoint(event.getMessage());
                if (!endpoint.isEmpty()) {
                    text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                            "  " + endpoint);
                }
            }
            case SERVER_STOPPED -> text.text(CodeColorScheme.TEXT, "Server stopped");
            case SERVER_FAILED -> {
                text.text(CodeColorScheme.NOTIFY_ERROR, "Server failed");
                detail = event.getDetail();
            }
            case AGENT_CONNECTED -> {
                text.text(CodeColorScheme.PARAM_REF, client);
                text.text(CodeColorScheme.TEXT, " connected");
                if (!event.getAgentVersion().isEmpty()) {
                    text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                            "  " + event.getAgentVersion());
                }
            }
            case AGENT_DISCONNECTED -> {
                text.text(CodeColorScheme.PARAM_REF, client);
                text.text(CodeColorScheme.TEXT, " disconnected");
            }
            case TOOL_CALLED -> {
                text.text(CodeColorScheme.PARAM_REF, client);
                text.text(CodeColorScheme.TEXT, " called ");
                text.text(CodeColorScheme.METHOD_REF, event.getToolName());
                if (!event.getDetail().isEmpty()) {
                    text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                            "  " + event.getDetail());
                }
            }
            case TOOL_RESULT -> {
                text.text(CodeColorScheme.METHOD_REF, event.getToolName());
                text.text(event.isSuccess() ? CodeColorScheme.TEXT : CodeColorScheme.NOTIFY_ERROR,
                        event.isSuccess() ? " completed" : " failed");
                text.text(CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, MUTED_ALPHA),
                        "  " + event.getDurationMillis() + " ms");
                if (!event.isSuccess()) detail = event.getDetail();
            }
        }
        return new EventPresentation(text.get(), detail, color);
    }

    private static int eventColor(McpActivityEvent event) {
        return switch (event.getType()) {
            case SERVER_STARTED, AGENT_CONNECTED -> CodeColorScheme.NOTIFY_SUCCESS;
            case SERVER_STOPPED, AGENT_DISCONNECTED -> CodeColorScheme.DISABLED;
            case SERVER_FAILED -> CodeColorScheme.NOTIFY_ERROR;
            case TOOL_CALLED -> CodeColorScheme.NOTIFY_INFORMATION;
            case TOOL_RESULT -> event.isSuccess()
                    ? CodeColorScheme.NOTIFY_SUCCESS : CodeColorScheme.NOTIFY_ERROR;
        };
    }

    private static String toPlainText(McpActivityEvent event) {
        EventPresentation presentation = presentation(event);
        StringBuilder text = new StringBuilder(formatTime(event.getTimestamp())).append("  ");
        for (ColoredString part : presentation.message()) text.append(part.getText());
        if (!presentation.detail().isEmpty()) text.append("  ").append(presentation.detail());
        return text.toString();
    }

    private static String buildAllText(McpActivityLog log) {
        if (log == null) return "";
        StringBuilder text = new StringBuilder();
        for (McpActivityEvent event : log.snapshotEvents()) {
            if (!text.isEmpty()) text.append('\n');
            text.append(toPlainText(event));
        }
        return text.toString();
    }

    private static String ellipsize(String text, float maximumWidth) {
        if (text.isEmpty() || ImGui.calcTextSize(text).x <= maximumWidth) return text;
        String suffix = "...";
        float available = Math.max(0.F, maximumWidth - ImGui.calcTextSize(suffix).x);
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (ImGui.calcTextSize(text.substring(0, middle)).x <= available) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, low) + suffix;
    }

    private static String extractEndpoint(String message) {
        int index = message.lastIndexOf("http");
        return index < 0 ? "" : message.substring(index).trim();
    }

    private static String displayClientName(String name) {
        return name == null || name.isBlank() ? "Client" : name;
    }

    private static String formatTime(long timestamp) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault()));
    }

    private static void pushCompactControls() {
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, COMPACT_PADDING_X, COMPACT_PADDING_Y);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, COMPACT_ROUNDING);
        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding,
                ImGui.getStyle().getCellPaddingX(), 0.F);
    }

    private static float compactControlHeight() {
        return ImGui.getFontSize() + COMPACT_PADDING_Y * 2.F;
    }

    private record EventPresentation(List<ColoredString> message, String detail, int color) { }
}
