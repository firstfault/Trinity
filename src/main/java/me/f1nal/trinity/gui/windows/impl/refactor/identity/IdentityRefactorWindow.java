package me.f1nal.trinity.gui.windows.impl.refactor.identity;

import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.refactor.identity.IdentityRefactorChange;
import me.f1nal.trinity.refactor.identity.IdentityRefactorIssue;
import me.f1nal.trinity.refactor.identity.IdentityRefactorPlan;
import me.f1nal.trinity.refactor.identity.IdentityRefactorSeverity;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Detailed review shown only when analysis found a conflict or a meaningful warning. */
public final class IdentityRefactorWindow extends ClosableWindow {
    private final IdentityRefactorPlan plan;
    private final ImString search = new ImString(192);
    private final ImInt category = new ImInt();
    private final List<IdentityRefactorChange> filtered = new ArrayList<>();
    private final TableComponent<IdentityRefactorChange> table = new TableComponent<>();
    private String previousSearch = "";
    private int previousCategory = -1;
    private boolean filterInitialized;

    IdentityRefactorWindow(Trinity trinity, IdentityRefactorPlan plan) {
        super("Review Bytecode Rename", 940.F, 620.F, trinity);
        this.plan = plan;
        this.setDialog(true);
        this.windowFlags |= ImGuiWindowFlags.NoResize;
        buildTable();
        refreshFilter();
    }

    @Override
    protected void renderFrame() {
        drawHeader();
        drawIssues();
        drawChangeToolbar();
        table.setElementList(filtered);
        table.draw(Math.max(80.F, ImGui.getContentRegionAvailY() - 38.F));
        ImGui.separator();

        if (plan.hasConflicts()) ImGui.beginDisabled();
        if (ImGui.button("Apply Rename")) {
            if (IdentityRefactorController.apply(plan)) close();
        }
        if (plan.hasConflicts()) ImGui.endDisabled();
        ImGui.sameLine();
        if (ImGui.button("Cancel")) close();
        if (plan.hasConflicts()) {
            ImGui.sameLine();
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR,
                    "Resolve the conflicts before applying this rename.");
        }
    }

    private void drawHeader() {
        ImGui.textUnformatted(plan.getRequest().kind().getDisplayName());
        ImGui.sameLine();
        ImGui.textColored(CodeColorScheme.DISABLED, plan.getRequest().oldIdentity());
        ImGui.sameLine();
        ImGui.textColored(CodeColorScheme.TEXT, "  ->  ");
        ImGui.sameLine();
        ImGui.textColored(plan.hasConflicts()
                        ? CodeColorScheme.NOTIFY_ERROR : CodeColorScheme.NOTIFY_SUCCESS,
                plan.getRequest().proposedIdentity());
        ImGui.textColored(CodeColorScheme.DISABLED,
                plan.getAffectedClasses().size() + " classes    "
                        + plan.getChanges().size() + " classfile values    "
                        + plan.getWarningCount() + " warnings    "
                        + plan.getConflictCount() + " conflicts");
        ImGui.separator();
    }

    private void drawIssues() {
        if (plan.getIssues().isEmpty()) return;
        float height = Math.min(174.F, 24.F + plan.getIssues().size() * 48.F);
        if (ImGui.beginChild(getId("Issues"), 0.F, height, true)) {
            for (IdentityRefactorIssue issue : plan.getIssues()) {
                int color = switch (issue.severity()) {
                    case CONFLICT -> CodeColorScheme.NOTIFY_ERROR;
                    case WARNING -> CodeColorScheme.NOTIFY_WARN;
                    case INFORMATION -> CodeColorScheme.NOTIFY_INFORMATION;
                };
                ImGui.textColored(color, issue.title());
                ImGui.indent(12.F);
                ImGui.pushTextWrapPos(ImGui.getCursorPosX() + ImGui.getContentRegionAvailX());
                ImGui.textColored(CodeColorScheme.DISABLED, issue.detail());
                ImGui.popTextWrapPos();
                ImGui.unindent(12.F);
                ImGui.spacing();
            }
        }
        ImGui.endChild();
        ImGui.spacing();
    }

    private void drawChangeToolbar() {
        ImGui.setNextItemWidth(260.F);
        if (ImGui.inputTextWithHint("###RefactorChangeSearch", "Filter changes", search,
                ImGuiInputTextFlags.None)) refreshFilter();
        ImGui.sameLine();
        ImGui.setNextItemWidth(170.F);
        String[] categories = categoryLabels();
        if (ImGui.combo("###RefactorCategory", category, categories)) refreshFilter();
        ImGui.sameLine();
        ImGui.textColored(CodeColorScheme.DISABLED,
                filtered.size() + (filtered.size() == 1 ? " change" : " changes"));
    }

    private void buildTable() {
        table.getColumns().add(new TableColumn<IdentityRefactorChange>("Kind",
                (column, change) -> drawCell(change, column.getHeader(),
                        change.category().getDisplayName(), categoryColor(change.category()), false))
                .setSortKey(change -> change.category().getDisplayName()).setWidthWeight(0.8F));
        table.getColumns().add(new TableColumn<IdentityRefactorChange>("Class",
                (column, change) -> drawCell(change, column.getHeader(), change.className(),
                        CodeColorScheme.CLASS_REF, true))
                .setSortKey(IdentityRefactorChange::className).setWidthWeight(1.4F));
        table.getColumns().add(new TableColumn<IdentityRefactorChange>("Location",
                (column, change) -> drawCell(change, column.getHeader(), change.location(),
                        CodeColorScheme.TEXT, false))
                .setSortKey(IdentityRefactorChange::location).setWidthWeight(2.1F));
        table.getColumns().add(new TableColumn<IdentityRefactorChange>("Before",
                (column, change) -> drawCell(change, column.getHeader(), change.before(),
                        CodeColorScheme.DISABLED, true))
                .setSortKey(IdentityRefactorChange::before).setWidthWeight(1.6F));
        table.getColumns().add(new TableColumn<IdentityRefactorChange>("After",
                (column, change) -> drawCell(change, column.getHeader(), change.after(),
                        CodeColorScheme.TEXT, true))
                .setSortKey(IdentityRefactorChange::after).setWidthWeight(1.6F));
    }

    private void drawCell(IdentityRefactorChange change, String column, String text,
                          int color, boolean keepEnd) {
        float availableWidth = Math.max(0.F,
                ImGui.getWindowDrawList().getClipRectMaxX()
                        - ImGui.getCursorScreenPosX()
                        - ImGui.getStyle().getCellPaddingX() - 1.F);
        String visibleText = ellipsize(text, availableWidth, keepEnd);
        boolean clipped = !visibleText.equals(text);
        ImGui.textColored(color, visibleText);

        if (clipped && ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getCursorPosX() + Math.min(720.F,
                    Math.max(240.F, ImGui.calcTextSize(text).x)));
            ImGui.textUnformatted(text);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
        }
        if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
            showCopyMenu(change, column, text);
        }
    }

    private void showCopyMenu(IdentityRefactorChange change, String column, String cellText) {
        PopupItemBuilder popup = PopupItemBuilder.create()
                .menuItem("Copy Entry", () -> SystemUtil.copyToClipboard(formatEntry(change)))
                .menuItem("Copy \"" + column + "\"",
                        () -> SystemUtil.copyToClipboard(cellText))
                .separator()
                .menuItem("Copy All Entries",
                        () -> SystemUtil.copyToClipboard(formatAllEntries()));
        Main.getDisplayManager().showPopup(popup);
    }

    private String formatAllEntries() {
        StringBuilder text = new StringBuilder("Kind\tClass\tLocation\tBefore\tAfter");
        for (IdentityRefactorChange change : filtered) {
            text.append('\n').append(formatEntry(change));
        }
        return text.toString();
    }

    private static String formatEntry(IdentityRefactorChange change) {
        return change.category().getDisplayName() + '\t'
                + change.className() + '\t'
                + change.location() + '\t'
                + change.before() + '\t'
                + change.after();
    }

    private static String ellipsize(String text, float maximumWidth, boolean keepEnd) {
        if (text.isEmpty() || ImGui.calcTextSize(text).x <= maximumWidth) return text;
        String ellipsis = "...";
        float remainingWidth = maximumWidth - ImGui.calcTextSize(ellipsis).x;
        if (remainingWidth <= 0.F) return ellipsis;

        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            String candidate;
            if (keepEnd) {
                int start = suffixStart(text, middle);
                candidate = ellipsis + text.substring(start);
            } else {
                int end = prefixEnd(text, middle);
                candidate = text.substring(0, end) + ellipsis;
            }
            if (ImGui.calcTextSize(candidate).x <= maximumWidth) low = middle;
            else high = middle - 1;
        }

        if (low == 0) return ellipsis;
        if (keepEnd) return ellipsis + text.substring(suffixStart(text, low));
        return text.substring(0, prefixEnd(text, low)) + ellipsis;
    }

    private static int suffixStart(String text, int characterCount) {
        int start = Math.max(0, text.length() - characterCount);
        if (start > 0 && start < text.length()
                && Character.isLowSurrogate(text.charAt(start))
                && Character.isHighSurrogate(text.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private static int prefixEnd(String text, int characterCount) {
        int end = Math.min(text.length(), characterCount);
        if (end > 0 && end < text.length()
                && Character.isHighSurrogate(text.charAt(end - 1))
                && Character.isLowSurrogate(text.charAt(end))) {
            end--;
        }
        return end;
    }

    private void refreshFilter() {
        String normalized = search.get().trim().toLowerCase(Locale.ROOT);
        int selected = category.get() - 1;
        if (filterInitialized
                && normalized.equals(previousSearch)
                && selected == previousCategory) return;
        filterInitialized = true;
        previousSearch = normalized;
        previousCategory = selected;
        filtered.clear();
        for (IdentityRefactorChange change : plan.getChanges()) {
            if (selected >= 0 && change.category().ordinal() != selected) continue;
            if (!normalized.isEmpty()
                    && !(change.className() + ' ' + change.location() + ' '
                    + change.before() + ' ' + change.after())
                    .toLowerCase(Locale.ROOT).contains(normalized)) continue;
            filtered.add(change);
        }
    }

    private static String[] categoryLabels() {
        String[] labels = new String[IdentityRefactorChange.Category.values().length + 1];
        labels[0] = "All changes";
        for (int index = 0; index < labels.length - 1; index++) {
            labels[index + 1] = IdentityRefactorChange.Category.values()[index].getDisplayName();
        }
        return labels;
    }

    private static int categoryColor(IdentityRefactorChange.Category category) {
        return switch (category) {
            case DECLARATION -> CodeColorScheme.TEXT;
            case BYTECODE -> CodeColorScheme.METHOD_REF;
            case DESCRIPTOR, SIGNATURE -> CodeColorScheme.CLASS_REF;
            case ANNOTATION -> CodeColorScheme.XREF_ANNOTATION;
            case METADATA, MODULE -> CodeColorScheme.DISABLED;
            case CONSTANT -> CodeColorScheme.STRING;
        };
    }
}
