package me.f1nal.trinity.gui.components.general.table;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.gui.components.filter.kind.IKindTypeName;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;

import java.util.Collections;
import java.util.function.Function;

public class TableColumnRendererXrefWhere<T extends IWhere & IKind> implements ITableCellRenderer<T> {
    private final boolean highlightOwnerClass;
    private final Function<T, PopupItemBuilder> contextMenuFactory;

    public TableColumnRendererXrefWhere() {
        this(false, null);
    }

    public TableColumnRendererXrefWhere(boolean highlightOwnerClass) {
        this(highlightOwnerClass, null);
    }

    public TableColumnRendererXrefWhere(boolean highlightOwnerClass,
                                        Function<T, PopupItemBuilder> contextMenuFactory) {
        this.highlightOwnerClass = highlightOwnerClass;
        this.contextMenuFactory = contextMenuFactory;
    }

    @Override
    public void render(TableColumn<T> column, T element) {
        String typeName = element instanceof IKindTypeName named
                ? named.getKindTypeName() : null;
        if (this.contextMenuFactory == null) {
            element.getWhere().draw(element.getKind(), Main.getDisplayManager().getPopupMenu(),
                    Main.getTrinity(), highlightOwnerClass,
                    typeName == null ? Collections.emptySet() : Collections.singleton(typeName));
        } else {
            element.getWhere().draw(element.getKind(), Main.getDisplayManager().getPopupMenu(),
                    Main.getTrinity(), highlightOwnerClass,
                    typeName == null ? Collections.emptySet() : Collections.singleton(typeName),
                    () -> this.contextMenuFactory.apply(element));
        }
    }
}
