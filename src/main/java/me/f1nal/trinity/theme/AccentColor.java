package me.f1nal.trinity.theme;

import me.f1nal.trinity.util.INameable;

import java.awt.Color;

public enum AccentColor implements INameable {
    SAPPHIRE("Sapphire", 82, 139, 255),
    GLACIER("Glacier", 91, 174, 225),
    LAGOON("Lagoon", 48, 174, 181),
    JADE("Jade", 70, 181, 139),
    SAGE("Sage", 130, 174, 112),
    CITRINE("Citrine", 218, 165, 74),
    EMBER("Ember", 229, 119, 87),
    ROSEWOOD("Rosewood", 213, 91, 118),
    ORCHID("Orchid", 196, 103, 188),
    VIOLET("Violet", 151, 116, 240),
    IRIS("Iris", 113, 126, 232);

    private final String name;
    private final int color;
    private final float[] rgba;

    AccentColor(String name, int red, int green, int blue) {
        this.name = name;
        this.color = CodeColorScheme.getRgb(new Color(red, green, blue));
        this.rgba = CodeColorScheme.toRgba(this.color);
    }

    @Override
    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public float[] getRgba() {
        return rgba.clone();
    }
}
