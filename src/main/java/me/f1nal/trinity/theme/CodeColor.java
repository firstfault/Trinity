package me.f1nal.trinity.theme;

import java.lang.reflect.Field;

public class CodeColor {
    private final String label;
    private final ThemeColorCategory category;
    private final Field field;

    public CodeColor(String label, ThemeColorCategory category, Field field) {
        this.label = label;
        this.category = category;
        this.field = field;
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
