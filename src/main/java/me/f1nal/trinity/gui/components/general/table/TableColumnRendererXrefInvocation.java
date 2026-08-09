package me.f1nal.trinity.gui.components.general.table;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;

import java.util.List;

/** Emphasizes hovered invocation types and supports a persistent type selection. */
public final class TableColumnRendererXrefInvocation implements ITableCellRenderer<AbstractXref> {
    private final TableTypeFocusController<AbstractXref, InvocationType> focus =
            new TableTypeFocusController<>();

    public void beginFrame(List<AbstractXref> xrefs) {
        this.focus.beginFrame(xrefs,
                xref -> resolveType(xref.getKind(), xref.getInvocation()));
    }

    @Override
    public void render(TableColumn<AbstractXref> column, AbstractXref xref) {
        String invocation = xref.getInvocation();
        InvocationType type = resolveType(xref.getKind(), invocation);
        int color = this.focus.getColor(type, CodeColorScheme.TEXT);
        String countText = " (" + this.focus.getCount(type) + ")";
        ImVec2 textSize = ImGui.calcTextSize(invocation);
        float countWidth = ImGui.calcTextSize(countText).x;
        boolean clicked = ImGui.invisibleButton("###XrefInvocation",
                Math.max(1.F, textSize.x + countWidth), ImGui.getTextLineHeight());
        if (ImGui.isItemHovered()) {
            this.focus.hover(xref, type);
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        }
        boolean showCount = this.focus.shouldShowCount(xref, type);
        ImVec2 textPosition = ImGui.getItemRectMin();
        ImGui.getWindowDrawList().addText(textPosition.x, textPosition.y, color, invocation);
        if (showCount) {
            ImGui.getWindowDrawList().addText(textPosition.x + textSize.x, textPosition.y,
                    CodeColorScheme.DISABLED, countText);
        }
        if (clicked) {
            this.focus.toggleSelection(xref, type);
        }
        if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
            Main.getDisplayManager().showPopup(this.createContextMenu(xref, false));
        }
    }

    public void endFrame() {
        this.focus.endFrame();
    }

    public List<AbstractXref> filterSelectedType(List<AbstractXref> xrefs) {
        return this.focus.filterSelected(xrefs,
                xref -> resolveType(xref.getKind(), xref.getInvocation()));
    }

    public PopupItemBuilder createContextMenu(AbstractXref xref, boolean whereColumn) {
        PopupItemBuilder popup = PopupItemBuilder.create();
        if (whereColumn) {
            addNavigationActions(popup, xref);
            popup.separator();
            addFilterAction(popup, xref);
        } else {
            addFilterAction(popup, xref);
            popup.separator();
            addNavigationActions(popup, xref);
        }

        popup.separator().menu("Copy", copy -> {
            if (whereColumn) {
                addCopyLocation(copy, xref);
                addCopyInvocation(copy, xref);
            } else {
                addCopyInvocation(copy, xref);
                addCopyLocation(copy, xref);
            }
            copy.menuItem("Row", () -> SystemUtil.copyToClipboard(
                    xref.getInvocation() + '\t' + xref.getWhere().getText()));
        });
        return popup;
    }

    private void addFilterAction(PopupItemBuilder popup, AbstractXref xref) {
        InvocationType type = resolveType(xref.getKind(), xref.getInvocation());
        boolean selected = this.focus.isSelected(type);
        popup.menuItem(selected ? "Show All Invocation Types" : "Show Only \"" + type.name() + "\"",
                () -> {
                    if (selected) this.focus.clearSelection();
                    else this.focus.select(xref, type);
                });
    }

    private static void addNavigationActions(PopupItemBuilder popup, AbstractXref xref) {
        popup.append(xref.getWhere().menuItem());
        Input<?> input = xref.getWhere().getInput();
        if (input != null) {
            popup.menu("Location Actions", input::populatePopup);
        }
    }

    private static void addCopyInvocation(PopupItemBuilder popup, AbstractXref xref) {
        popup.menuItem("Invocation", () -> SystemUtil.copyToClipboard(xref.getInvocation()));
    }

    private static void addCopyLocation(PopupItemBuilder popup, AbstractXref xref) {
        popup.menuItem("Location", () -> SystemUtil.copyToClipboard(xref.getWhere().getText()));
    }

    private static InvocationType resolveType(XrefKind kind, String invocation) {
        List<String> typeNames = kind.getTypeNames();
        for (String typeName : typeNames) {
            if (kind.matchesTypeName(typeName, invocation)) {
                return new InvocationType(kind, typeName);
            }
        }
        return new InvocationType(kind, invocation);
    }

    private record InvocationType(XrefKind kind, String name) {
    }
}
