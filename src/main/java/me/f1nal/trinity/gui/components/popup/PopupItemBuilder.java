package me.f1nal.trinity.gui.components.popup;

import me.f1nal.trinity.gui.components.popup.items.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PopupItemBuilder {
    private final List<PopupItem> items = new ArrayList<>();

    private PopupItemBuilder() {

    }

    public static PopupItemBuilder create() {
        return new PopupItemBuilder();
    }

    public PopupItemBuilder separator() {
        if (this.items.isEmpty()) {
            return this;
        }
        return this.add(new PopupItemSeparator());
    }

    public PopupItemBuilder menuItem(String label, Runnable event) {
        return this.menuItem(label, false, event);
    }

    public PopupItemBuilder menuItem(String label, boolean selected, Runnable event) {
        return this.menuItem(label, "", selected, event);
    }

    public PopupItemBuilder menuItem(String label, String shortcut, boolean selected, Runnable event) {
        return this.add(new PopupItemMenuItem(label, shortcut, selected, event));
    }

    public PopupItemBuilder menuItem(String label, String shortcut, Runnable event) {
        return this.add(new PopupItemMenuItem(label, shortcut, false, event));
    }

    public PopupItemBuilder predicate(BooleanSupplier predicate, Consumer<PopupItemBuilder> items) {
        PopupItemBuilder builder = PopupItemBuilder.create();
        items.accept(builder);
        return this.add(new PopupItemPredicate(predicate, builder.get(), false));
    }

    public PopupItemBuilder disabled(BooleanSupplier predicate, Consumer<PopupItemBuilder> items) {
        PopupItemBuilder builder = PopupItemBuilder.create();
        items.accept(builder);
        return this.add(new PopupItemPredicate(predicate, builder.get(), true));
    }

    public PopupItemBuilder menu(String label, Consumer<PopupItemBuilder> menu) {
        PopupItemBuilder builder = PopupItemBuilder.create();
        menu.accept(builder);
        return this.add(new PopupItemMenu(label, builder.get()));
    }

    private PopupItemBuilder add(PopupItem item) {
        this.items.add(item);
        return this;
    }

    public List<PopupItem> get() {
        return items;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }
}
