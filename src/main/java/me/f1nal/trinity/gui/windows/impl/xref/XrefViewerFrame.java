package me.f1nal.trinity.gui.windows.impl.xref;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class XrefViewerFrame extends ClosableWindow {
    private final Collection<AbstractXref> xrefViewList;
    private final PopupMenu popupMenu = new PopupMenu();
    private final XrefBuilder builder;
    private final ListFilterComponent<AbstractXref> listFilterComponent;

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity, boolean autofollowXref) {
        super("", 680, 300, trinity);

        this.xrefViewList = builder.createXrefs();
        this.listFilterComponent = new ListFilterComponent<>(this.xrefViewList, new SearchBarFilter<>(), new KindFilter<>(XrefKind.values()));
        this.builder = builder;
        this.setCloseableByEscape(true);

        if (autofollowXref && this.xrefViewList.size() == 1) this.followFirstXref();
    }

    private void followFirstXref() {
        if (Main.getPreferences().isAutoviewXref()) {
            this.close();
            for (AbstractXref xref : this.xrefViewList) {
                xref.getWhere().followInDecompiler();
                break;
            }
        }
    }

    public XrefViewerFrame(XrefBuilder builder, Trinity trinity) {
        this(builder, trinity, true);
    }

    @Override
    public String getTitle() {
        return "Xref Viewer: " + builder.getTitle();
    }

    @Override
    protected void renderFrame() {
        this.listFilterComponent.draw();
        drawTable(this.listFilterComponent.getFilteredList());
    }

    protected void drawTable(List<AbstractXref> sortedList) {
        if (!ImGui.beginTable("xref viewer table" + getId(), 3, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingFixedFit | ImGuiTableFlags.Sortable)) {
            return;
        }
        ImGui.tableSetupColumn("Access", ImGuiTableColumnFlags.NoResize);
        ImGui.tableSetupColumn("Invocation", ImGuiTableColumnFlags.NoResize);
        ImGui.tableSetupColumn("Where");
        ImGui.tableHeadersRow();
        for (int i = 0, xrefsLength = Main.getPreferences().getSearchLimit(sortedList.size()); i < xrefsLength; i++) {
            AbstractXref xref = sortedList.get(i);
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(0);
            ImGui.text(xref.getAccess().getText());
            ImGui.tableSetColumnIndex(1);
            ImGui.text(xref.getInvocation());
            ImGui.tableSetColumnIndex(2);
            xref.getWhere().draw(xref.getKind(), popupMenu, trinity);
        }
        ImGui.endTable();
        popupMenu.draw();
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof XrefViewerFrame && ((XrefViewerFrame) otherWindow).builder.equals(this.builder);
    }
}
