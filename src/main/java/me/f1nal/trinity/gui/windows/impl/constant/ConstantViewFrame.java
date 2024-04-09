package me.f1nal.trinity.gui.windows.impl.constant;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.gui.components.filter.ListFilterComponent;
import me.f1nal.trinity.gui.components.filter.SearchBarFilter;
import me.f1nal.trinity.gui.components.filter.kind.KindFilter;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;

import java.util.List;

public class ConstantViewFrame extends ClosableWindow {
    private final PopupMenu popupMenu = new PopupMenu();
    private final List<ConstantViewCache> constantList;
    private final ListFilterComponent<ConstantViewCache> listFilterComponent;

    public ConstantViewFrame(Trinity trinity, List<ConstantViewCache> constantList) {
        super("Constant Viewer", 680, 300, trinity);
        this.constantList = constantList;
        this.listFilterComponent = new ListFilterComponent<>(this.constantList, new SearchBarFilter<>(), new KindFilter<>(XrefKind.values()));
        this.setCloseableByEscape(true);
    }

    @Override
    protected void renderFrame() {
        listFilterComponent.draw();

        this.drawTable(listFilterComponent.getFilteredList());
    }

    protected void drawTable(List<ConstantViewCache> sortedList) {
        if (!ImGui.beginTable("constant viewer table" + getId(), 3, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingFixedFit | ImGuiTableFlags.Sortable)) {
            return;
        }
        ImGui.tableSetupColumn("Constant", ImGuiTableColumnFlags.NoResize);
        ImGui.tableSetupColumn("Where");
        ImGui.tableHeadersRow();
        for (int i = 0, length = Main.getPreferences().getSearchLimit(sortedList.size()); i < length; i++) {
            ConstantViewCache where = sortedList.get(i);
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(0);
            ImGui.text(where.getConstant());
            ImGui.tableSetColumnIndex(1);
            where.getWhere().draw(where.getKind(), popupMenu, trinity);
        }
        ImGui.endTable();
        popupMenu.draw();
    }
}
