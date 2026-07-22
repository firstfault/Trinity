package me.f1nal.trinity.gui.actions;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A reusable application-level command shared by menus and global search. */
public record ApplicationAction(String id, String title, String description, String category,
                                String icon, List<String> aliases,
                                BooleanSupplier availability, Runnable executor) {
    public ApplicationAction {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        Objects.requireNonNull(category);
        Objects.requireNonNull(icon);
        aliases = List.copyOf(aliases);
        Objects.requireNonNull(availability);
        Objects.requireNonNull(executor);
    }

    public boolean isAvailable() {
        return availability.getAsBoolean();
    }

    public void execute() {
        if (this.isAvailable()) executor.run();
    }

    public String getMenuLabel() {
        return icon.isEmpty() ? title : icon + " " + title;
    }
}
