package me.f1nal.trinity.gui.components.general.table;

import imgui.ImGui;
import imgui.ImGuiTableColumnSortSpecs;
import imgui.ImGuiTableSortSpecs;
import imgui.flag.ImGuiSortDirection;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.ComponentId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
                | ImGuiTableFlags.Sortable | ImGuiTableFlags.SortTristate
                | ImGuiTableFlags.Resizable;
        if (height > 0.F) {
            flags |= ImGuiTableFlags.ScrollY;
        }
        if (!ImGui.beginTable(this.id, this.columns.size(), flags, 0.F, height)) {
            return;
        }

        for (TableColumn<T> column : this.columns) {
            ImGui.tableSetupColumn(column.getHeader(), column.getFlags(), column.getWidthWeight());
        }

        if (height > 0.F) {
            ImGui.tableSetupScrollFreeze(0, 1);
        }
        ImGui.tableHeadersRow();
        List<T> displayedElements = this.getSortedElements(ImGui.tableGetSortSpecs());
        for (int j = 0, size = Math.min(displayedElements.size(), Main.getPreferences().getSearchMaxDisplay().getMax()); j < size; j++) {
            T element = displayedElements.get(j);

            ImGui.tableNextRow();
            ImGui.pushID(j);
            for (int i = 0; i < this.columns.size(); i++) {
                ImGui.tableSetColumnIndex(i);
                this.columns.get(i).draw(element);
            }
            ImGui.popID();
        }
        ImGui.endTable();
    }

    private List<T> getSortedElements(ImGuiTableSortSpecs sortSpecs) {
        if (sortSpecs == null || sortSpecs.getSpecsCount() == 0) {
            return this.elementList;
        }

        Comparator<T> comparator = null;
        ImGuiTableColumnSortSpecs[] columnSpecs = sortSpecs.getSpecs();
        Arrays.sort(columnSpecs, Comparator.comparingInt(ImGuiTableColumnSortSpecs::getSortOrder));
        for (ImGuiTableColumnSortSpecs columnSpec : columnSpecs) {
            int columnIndex = columnSpec.getColumnIndex();
            if (columnIndex < 0 || columnIndex >= this.columns.size()) continue;

            Comparator<T> columnComparator = this.columns.get(columnIndex).getComparator();
            if (columnComparator == null
                    || columnSpec.getSortDirection() == ImGuiSortDirection.None) {
                continue;
            }
            if (columnSpec.getSortDirection() == ImGuiSortDirection.Descending) {
                columnComparator = columnComparator.reversed();
            }
            comparator = comparator == null
                    ? columnComparator : comparator.thenComparing(columnComparator);
        }
        sortSpecs.setSpecsDirty(false);
        if (comparator == null) return this.elementList;

        List<T> sorted = new ArrayList<>(this.elementList);
        sorted.sort(comparator);
        return sorted;
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
