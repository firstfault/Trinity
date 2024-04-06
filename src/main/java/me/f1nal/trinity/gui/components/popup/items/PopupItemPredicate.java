package me.f1nal.trinity.gui.components.popup.items;

import java.util.List;
import java.util.function.BooleanSupplier;

public class PopupItemPredicate extends PopupItem {
    private final BooleanSupplier predicate;
    private final List<PopupItem> items;

    public PopupItemPredicate(BooleanSupplier predicate, List<PopupItem> items) {
        this.predicate = predicate;
        this.items = items;
    }

    @Override
    public void draw() {
        if (predicate.getAsBoolean()) {
            items.forEach(PopupItem::draw);
        }
    }
}
