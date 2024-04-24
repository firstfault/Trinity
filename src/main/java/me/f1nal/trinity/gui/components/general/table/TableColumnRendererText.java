package me.f1nal.trinity.gui.components.general.table;

import imgui.ImGui;

import java.util.function.Function;

public class TableColumnRendererText<T> implements ITableCellRenderer<T> {
    private final Function<T, String> function;

    public TableColumnRendererText(Function<T, String> function) {
        this.function = function;
    }

    @Override
    public void render(TableColumn<T> column, T element) {
        ImGui.text(function.apply(element));
    }
}
