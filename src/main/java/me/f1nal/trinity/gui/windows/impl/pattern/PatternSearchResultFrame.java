package me.f1nal.trinity.gui.windows.impl.pattern;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.pattern.InstructionPatternMatch;
import me.f1nal.trinity.gui.components.FontSettings;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableColumnRendererXrefWhere;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.List;

public final class PatternSearchResultFrame extends ClosableWindow {
    private static final int TITLE_LIMIT = 70;
    private final ListFilterComponent<PatternResultRow> filter;
    private final TableComponent<PatternResultRow> table = new TableComponent<>();
    private final long totalMatches;
    private final int retainedMatches;

    public PatternSearchResultFrame(Trinity trinity, List<InstructionPatternMatch> matches,
                                    long totalMatches, String patternSource) {
        super(title(patternSource), 760, 340, trinity);
        String patternSummary = summary(patternSource, 80);
        List<PatternResultRow> rows = matches.stream()
                .map(match -> new PatternResultRow(match, patternSummary)).toList();
        this.filter = new ListFilterComponent<>(rows, new SearchBarFilter<>(true));
        this.totalMatches = totalMatches;
        this.retainedMatches = matches.size();
        this.table.getColumns().add(new TableColumn<>("Match", (column, row) -> {
            FontSettings font = Main.getPreferences().getDecompilerFont();
            font.pushFont();
            ImGui.textColored(CodeColorScheme.KEYWORD_DATA, row.getSummary());
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                for (String line : row.getMatch().formattedInstructions().split("\\R")) {
                    ImGui.textColored(CodeColorScheme.TEXT, line);
                }
                ImGui.endTooltip();
            }
            font.popFont();
        }));
        this.table.getColumns().add(new TableColumn<>("Where", new TableColumnRendererXrefWhere<>()));
        this.setCloseableByEscape(true);
        this.setInitialPositionAtMouse();
    }

    @Override
    protected void renderFrame() {
        filter.draw();
        if (totalMatches > retainedMatches) {
            ImGui.textColored(CodeColorScheme.DISABLED, totalMatches + " matches; showing first " + retainedMatches);
        } else {
            ImGui.textColored(CodeColorScheme.DISABLED, totalMatches + (totalMatches == 1 ? " match" : " matches"));
        }
        table.setElementList(filter.getFilteredList());
        table.draw(Math.max(1.F, ImGui.getContentRegionAvailY()));
    }

    private static String title(String source) {
        return "Pattern Results: " + summary(source, TITLE_LIMIT);
    }

    private static String summary(String source, int limit) {
        String value = source == null ? "" : source.lines()
                .map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .reduce((left, right) -> left + " -> " + right).orElse("Instructions");
        return value.length() <= limit ? value : value.substring(0, Math.max(0, limit - 3)) + "...";
    }
}
