package me.f1nal.trinity.execution.compile;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Console {
    private static final AtomicLong NEXT_LOG_ID = new AtomicLong();

    private final List<LogEntry> logs = new ArrayList<>();

    public void draw() {
        List<LogSnapshot> snapshot;
        synchronized (logs) {
            snapshot = logs.stream().map(LogEntry::snapshot).toList();
        }
        for (LogSnapshot log : snapshot) {
            ColoredString summary = log.summary();
            List<ColoredString> details = log.details();
            if (details == null) {
                ImGui.textColored(summary.getColor(), summary.getText());
                continue;
            }

            ImGui.pushStyleColor(ImGuiCol.Text, summary.getColor());
            boolean open = ImGui.treeNode(summary.getText() + "###ConsoleLog" + log.id());
            ImGui.popStyleColor();
            if (open) {
                for (int detailIndex = 0; detailIndex < details.size(); detailIndex++) {
                    ColoredString detail = details.get(detailIndex);
                    ImGui.textColored(CodeColorScheme.TEXT, detail.getText());
                    if (ImGui.beginPopupContextItem(
                            "##ConsoleLogDetail" + log.id() + ":" + detailIndex)) {
                        if (ImGui.menuItem("Copy text")) {
                            SystemUtil.copyToClipboard(detail.getText());
                        }
                        ImGui.endPopup();
                    }
                }
                ImGui.treePop();
            }
        }
    }

    private void addLog(int color, String info) {
        synchronized (logs) {
            this.logs.add(0, new LogEntry(color, info, null));
        }
    }

    public void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }

    public boolean isEmpty() {
        synchronized (logs) {
            return logs.isEmpty();
        }
    }

    public String getPlainText() {
        synchronized (logs) {
            return logs.stream().map(log -> {
                StringBuilder text = new StringBuilder(log.summary.getText());
                if (log.details != null) {
                    log.details.forEach(detail -> text.append(System.lineSeparator())
                            .append("  ").append(detail.getText()));
                }
                return text.toString();
            }).collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public void error(String info, String... args) {
        this.addLog(CodeColorScheme.NOTIFY_ERROR, fmt(info, args));
    }

    public void warn(String info, String... args) {
        this.addLog(CodeColorScheme.NOTIFY_WARN, fmt(info, args));
    }

    public ExpandableLog warnExpandable(String info, Collection<String> details, String... args) {
        LogEntry entry = new LogEntry(CodeColorScheme.NOTIFY_WARN, fmt(info, args),
                coloredDetails(CodeColorScheme.NOTIFY_WARN, details));
        synchronized (logs) {
            this.logs.add(0, entry);
        }
        return new ExpandableLog(entry);
    }

    private static List<ColoredString> coloredDetails(int color, Collection<String> details) {
        return details.stream().map(detail -> new ColoredString(detail, color)).toList();
    }

    private static String fmt(String format, String[] args) {
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex]);
                    argIndex++;
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    public void info(String info, String... args) {
        this.addLog(CodeColorScheme.TEXT, fmt(info, args));
    }

    public final class ExpandableLog {
        private final LogEntry entry;

        private ExpandableLog(LogEntry entry) {
            this.entry = entry;
        }

        public void update(String info, Collection<String> details, String... args) {
            synchronized (logs) {
                entry.setSummary(new ColoredString(fmt(info, args), CodeColorScheme.NOTIFY_WARN));
                entry.setDetails(coloredDetails(CodeColorScheme.NOTIFY_WARN, details));
            }
        }

        String getSummary() {
            synchronized (logs) {
                return entry.summary.getText();
            }
        }

        List<String> getDetails() {
            synchronized (logs) {
                return entry.details.stream().map(ColoredString::getText).toList();
            }
        }
    }

    private static final class LogEntry {
        private final long id = NEXT_LOG_ID.incrementAndGet();
        private ColoredString summary;
        private List<ColoredString> details;

        private LogEntry(int color, String summary, List<ColoredString> details) {
            this.summary = new ColoredString(summary, color);
            this.details = details;
        }

        private void setSummary(ColoredString summary) {
            this.summary = summary;
        }

        private void setDetails(List<ColoredString> details) {
            this.details = details;
        }

        private LogSnapshot snapshot() {
            return new LogSnapshot(id, summary, details);
        }
    }

    private record LogSnapshot(long id, ColoredString summary, List<ColoredString> details) {
    }
}
