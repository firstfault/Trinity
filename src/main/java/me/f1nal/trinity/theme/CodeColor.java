package me.f1nal.trinity.theme;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CodeColor {
    private final String label;
    private final ThemeColorCategory category;
    private final Field field;
    private final List<Runnable> listeners = new ArrayList<>(1);

    public CodeColor(String label, ThemeColorCategory category, Field field) {
        this.label = label;
        this.category = category;
        this.field = field;
    }

    public List<Runnable> getListeners() {
        return listeners;
    }

    public int getColor() {
        try {
            return (int) field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Getting color", e);
        }
    }

    public void setColor(int color) {
        try {
            field.set(null, color);

            for (Runnable listener : listeners) {
                listener.run();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Setting color", e);
        }
    }

    public String getLabel() {
        return label;
    }

    public ThemeColorCategory getCategory() {
        return category;
    }

    public Field getField() {
        return field;
    }
}
