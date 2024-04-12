package me.f1nal.trinity.gui.components.popup.items;

import me.f1nal.trinity.gui.components.popup.PopupMenuState;
import me.f1nal.trinity.util.GuiUtil;

import java.util.List;
import java.util.function.BooleanSupplier;

public class PopupItemPredicate extends PopupItem {
    private final BooleanSupplier predicate;
    private final List<PopupItem> items;
    private final boolean disabled;

    public PopupItemPredicate(BooleanSupplier predicate, List<PopupItem> items, boolean disabled) {
        this.predicate = predicate;
        this.items = items;
        this.disabled = disabled;
    }

    @Override
    public void draw(PopupMenuState state) {
        boolean enabled = predicate.getAsBoolean();

        if (this.disabled) {
            GuiUtil.disabledWidget(enabled, () -> drawItems(state));
            return;
        }

        if (enabled) {
            this.drawItems(state);
        }
    }

    private void drawItems(PopupMenuState state) {
        items.forEach(popupItem -> popupItem.draw(state));
    }
}
