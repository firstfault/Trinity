package me.f1nal.trinity.gui.components.general.table;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.filter.kind.IKind;

public class TableColumnRendererXrefWhere<T extends IWhere & IKind> implements ITableCellRenderer<T> {
    @Override
    public void render(TableColumn<T> column, T element) {
        element.getWhere().draw(element.getKind(), Main.getDisplayManager().getPopupMenu(), Main.getTrinity());
    }
}
