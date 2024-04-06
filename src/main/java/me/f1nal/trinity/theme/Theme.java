package me.f1nal.trinity.theme;

import me.f1nal.trinity.appdata.ThemeFile;
import me.f1nal.trinity.util.INameable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void readFrom(ThemeFile themeFile) {
        for (ThemeColor color : colors) {
            Integer rgb = themeFile.getColors().get(color.getLabel());
            if (rgb == null) {
                continue;
            }
            color.setRgba(CodeColorScheme.toRgba(rgb));
        }
    }
}
