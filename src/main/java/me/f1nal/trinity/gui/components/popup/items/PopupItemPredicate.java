package me.f1nal.trinity.gui.components.popup.items;

import imgui.ImGui;
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
    public void draw() {
        boolean state = predicate.getAsBoolean();

        if (this.disabled) {
            GuiUtil.disabledWidget(state, this::drawItems);
            return;
        }

        if (state) {
            this.drawItems();
        }
    }

    private void drawItems() {
        items.forEach(PopupItem::draw);
    }
}
