package me.f1nal.trinity.gui.components.general.table;

import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.ComponentId;

import java.util.ArrayList;
import java.util.List;

public class TableComponent<T> {
    private final String id = ComponentId.getId(this.getClass());
    private final List<TableColumn<T>> columns = new ArrayList<>(2);
    private List<T> elementList;

    public TableComponent(List<T> elementList) {
        this.elementList = elementList;
    }

    public TableComponent() {
        this(null);
    }

    public void draw() {
        this.draw(0.F);
    }

    public void draw(float height) {
        int flags = ImGuiTableFlags.Borders | ImGuiTableFlags.SizingStretchProp
                | ImGuiTableFlags.Sortable | ImGuiTableFlags.Resizable;
        if (height > 0.F) {
            flags |= ImGuiTableFlags.ScrollY;
        }
        if (!ImGui.beginTable(this.id, this.columns.size(), flags, 0.F, height)) {
            return;
        }

        for (TableColumn<T> column : this.columns) {
            ImGui.tableSetupColumn(column.getHeader(), column.getFlags());
        }

        if (height > 0.F) {
            ImGui.tableSetupScrollFreeze(0, 1);
        }
        ImGui.tableHeadersRow();
        for (int j = 0, size = Math.min(this.elementList.size(), Main.getPreferences().getSearchMaxDisplay().getMax()); j < size; j++) {
            T element = this.elementList.get(j);

            ImGui.tableNextRow();
            for (int i = 0; i < this.columns.size(); i++) {
                ImGui.tableSetColumnIndex(i);
                this.columns.get(i).draw(element);
            }
        }
        ImGui.endTable();
    }

    public void setElementList(List<T> elementList) {
        this.elementList = elementList;
    }

    public List<T> getElementList() {
        return elementList;
    }

    public List<TableColumn<T>> getColumns() {
        return columns;
    }
}
