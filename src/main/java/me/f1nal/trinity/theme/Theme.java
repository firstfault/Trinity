package me.f1nal.trinity.theme;

import me.f1nal.trinity.appdata.ThemeFileNew;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.util.INameable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.f1nal.trinity.appdata.ThemeFileNew.*;

public class Theme implements INameable {
    private final String name;
    private final List<ThemeColor> colors = new ArrayList<>();
    private final Map<ThemeColorCategory, List<ThemeColor>> colorMap = new HashMap<>();
    private final boolean editable;

    public Theme(String name, boolean editable) {
        this.name = name;
        this.editable = editable;

        for (CodeColor codeColor : CodeColorScheme.getCodeColors()) {
            ThemeColor themeColor = new ThemeColor(codeColor.getLabel(), codeColor.getColor(), codeColor);
            colors.add(themeColor);
            colorMap.computeIfAbsent(codeColor.getCategory(), k -> new ArrayList<>()).add(themeColor);
        }
    }

    public Map<ThemeColorCategory, List<ThemeColor>> getColorMap() {
        return colorMap;
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public String getName() {
        return name;
    }

    public List<ThemeColor> getColors() {
        return colors;
    }

    public void readFrom(ThemeFileNew themeFile) {
        List<ColorCategory> categories = themeFile.getCategories();
        for (ColorCategory category : categories) {
            for (ThemeFileNew.Color color : category.getColors()) {
                ThemeColor themeColor = this.getColor(category.getName(), color.getName());

                if (themeColor == null) {
                    Logging.warn("No theme color for '{}' in category '{}'", color.getName(), category.getName());
                    continue;
                }

                themeColor.setRgba(toRgba(color.getColor()));
            }
        }
    }

    private static float[] toRgba(int in) {
        return new float[] {
                ((in >> 24) & 0xFF)/ 255.F,
                ((in >> 16) & 0xFF) / 255.F,
                ((in >> 8) & 0xFF)/ 255.F,
                ((in >> 0) & 0xFF) / 255.F,
        };
    }

    private ThemeColor getColor(String categoryName, String colorName) {
        for (ThemeColor color : colors) {
            if (color.getLabel().equals(colorName) && color.getCodeColor().getCategory().getName().equals(categoryName)) {
                return color;
            }
        }
        return null;
    }
}
