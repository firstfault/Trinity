package me.f1nal.trinity.gui.windows.impl.variablexref;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.DecompilerVariableAccess;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.general.table.TableColumn;
import me.f1nal.trinity.gui.components.general.table.TableComponent;
import me.f1nal.trinity.gui.components.general.table.TableTypeFocusController;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLineText;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.Comparator;
import java.util.List;

public final class VariableXrefViewerFrame extends ClosableWindow {
    private final DecompiledClass decompiledClass;
    private final MethodInput methodInput;
    private final int variableIndex;
    private final List<VariableXrefRow> rows;
    private final SearchBarFilter<VariableXrefRow> searchFilter = new SearchBarFilter<>();
    private final KindFilter<VariableXrefRow> accessFilter =
            new KindFilter<>(DecompilerVariableAccess.values());
    private final ListFilterComponent<VariableXrefRow> filters;
    private final TableComponent<VariableXrefRow> table = new TableComponent<>();
    private final TableTypeFocusController<VariableXrefRow, DecompilerVariableAccess> accessFocus =
            new TableTypeFocusController<>();

    public VariableXrefViewerFrame(Trinity trinity, DecompiledClass decompiledClass,
                                   MethodInput methodInput, int variableIndex) {
        super("", 680, 300, trinity);
        this.decompiledClass = decompiledClass;
        this.methodInput = methodInput;
        this.variableIndex = variableIndex;
        trinity.getDecompiler().refreshRenderedText(decompiledClass);
        decompiledClass.applyPendingOutput();
        this.rows = decompiledClass.getVariableReferences(methodInput, variableIndex).stream()
                .map(reference -> new VariableXrefRow(decompiledClass, reference))
                .toList();
        this.filters = new ListFilterComponent<>(rows, searchFilter, accessFilter);

        this.table.getColumns().add(new TableColumn<VariableXrefRow>("Access",
                (column, row) -> drawAccessCell(row))
                .setSortKey(row -> row.reference().access().getName())
                .setWidthWeight(1.F));
        this.table.getColumns().add(new TableColumn<VariableXrefRow>("Where",
                (column, row) -> drawSourceCell(row))
                .setComparator(Comparator.comparingInt(row -> row.reference().lineNumber()))
                .setWidthWeight(4.F));
        this.setDialog(true);
    }

    private void drawAccessCell(VariableXrefRow row) {
        DecompilerVariableAccess access = row.reference().access();
        String text = access.getName();
        String countText = " (" + this.accessFocus.getCount(access) + ")";
        float height = ImGui.getTextLineHeight();
        boolean clicked = ImGui.invisibleButton("##VariableXrefAccess",
                Math.max(1.F, ImGui.getContentRegionAvailX()), height);
        boolean hovered = ImGui.isItemHovered();
        if (hovered) {
            this.accessFocus.hover(row, access);
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        }
        boolean showCount = this.accessFocus.shouldShowCount(row, access);
        ImVec2 min = ImGui.getItemRectMin();
        int color = this.accessFocus.getColor(access, access.getColor());
        ImGui.getWindowDrawList().addText(min.x, min.y, color, text);
        if (showCount) {
            ImGui.getWindowDrawList().addText(
                    min.x + ImGui.calcTextSize(text).x, min.y,
                    CodeColorScheme.DISABLED, countText);
        }
        if (hovered) row.location().hover();
        if (clicked) this.accessFocus.toggleSelection(row, access);
        if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
            Main.getDisplayManager().getPopupMenu().show(row.location().menuItem());
        }
    }

    private void drawSourceCell(VariableXrefRow row) {
        float height = ImGui.getTextLineHeight();
        ImGui.invisibleButton("##VariableXrefWhere",
                Math.max(1.F, ImGui.getContentRegionAvailX()), height);
        ImVec2 min = ImGui.getItemRectMin();
        String lineNumber = Integer.toString(row.reference().lineNumber());
        ImGui.getWindowDrawList().addText(min.x, min.y, CodeColorScheme.DISABLED, lineNumber);

        float x = min.x + ImGui.calcTextSize(lineNumber).x + 10.F;
        int leadingWhitespace = row.reference().lineText().length()
                - row.reference().lineText().stripLeading().length();
        for (DecompilerLineText lineText : row.reference().lineComponents()) {
            String text = lineText.getText();
            if (leadingWhitespace > 0) {
                int skipped = Math.min(leadingWhitespace, text.length());
                text = text.substring(skipped);
                leadingWhitespace -= skipped;
            }
            if (text.isEmpty()) continue;
            int textColor = lineText.getComponent().getColor();
            float textWidth = ImGui.calcTextSize(text).x;
            if (lineText.getComponent() == row.reference().accessComponent()) {
                float padding = 1.F;
                ImGui.getWindowDrawList().addRectFilled(
                        x - padding, min.y - padding,
                        x + textWidth + padding, min.y + height + padding,
                        CodeColorScheme.setAlpha(textColor, 38));
                ImGui.getWindowDrawList().addRect(
                        x - padding, min.y - padding,
                        x + textWidth + padding, min.y + height + padding,
                        CodeColorScheme.setAlpha(textColor, 125), 0.F);
            }
            ImGui.getWindowDrawList().addText(x, min.y, textColor, text);
            x += textWidth;
        }
        row.location().controls(Main.getDisplayManager().getPopupMenu(), trinity);
    }

    @Override
    public String getTitle() {
        String variable = methodInput.getVariableTable().getVariable(variableIndex).getName();
        return "Variable Xrefs: " + methodInput.getOwningClass().getDisplaySimpleName() + "."
                + methodInput.getDisplayName().getName() + " - " + variable;
    }

    @Override
    protected void renderFrame() {
        this.filters.draw();
        List<VariableXrefRow> filtered = this.filters.getFilteredList();
        if (this.rows.isEmpty()) {
            ImGui.textDisabled("No reads or writes found.");
            return;
        }
        if (filtered.isEmpty()) {
            ImGui.textDisabled("No matching accesses.");
            return;
        }
        this.accessFocus.beginFrame(filtered, row -> row.reference().access());
        this.table.setElementList(this.accessFocus.filterSelected(
                filtered, row -> row.reference().access()));
        this.table.draw(Math.max(1.F, ImGui.getContentRegionAvailY()));
        this.accessFocus.endFrame();
    }

    @Override
    protected void onOpen() {
        this.searchFilter.getSearchBar().requestFocus();
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof VariableXrefViewerFrame other
                && other.methodInput.getNode() == this.methodInput.getNode()
                && other.variableIndex == this.variableIndex;
    }
}
