package me.f1nal.trinity.gui.components.general.table;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.gui.components.filter.kind.IKindTypeName;

import java.util.Collections;

public class TableColumnRendererXrefWhere<T extends IWhere & IKind> implements ITableCellRenderer<T> {
    private final boolean highlightOwnerClass;

    public TableColumnRendererXrefWhere() {
        this(false);
    }

    public TableColumnRendererXrefWhere(boolean highlightOwnerClass) {
        this.highlightOwnerClass = highlightOwnerClass;
    }

    @Override
    public void render(TableColumn<T> column, T element) {
        String typeName = element instanceof IKindTypeName named
                ? named.getKindTypeName() : null;
        element.getWhere().draw(element.getKind(), Main.getDisplayManager().getPopupMenu(),
                Main.getTrinity(), highlightOwnerClass,
                typeName == null ? Collections.emptySet() : Collections.singleton(typeName));
    }
}
