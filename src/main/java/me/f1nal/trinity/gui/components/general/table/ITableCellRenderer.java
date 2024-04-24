package me.f1nal.trinity.gui.components.general.table;

public interface ITableCellRenderer<T> {
    void render(TableColumn<T> column, T element);
}
