package me.f1nal.trinity.appdata;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import imgui.ImColor;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POJO for serializing/deserializing a theme from disk.
 */
@XStreamAlias("theme")
public class ThemeFileNew {
    @XStreamOmitField
    private static final XStream stream = new XStream();
    @XStreamImplicit
    private List<ColorCategory> categories;

    public ThemeFileNew() {
        this.categories = new ArrayList<>();
    }

    public ThemeFileNew(Theme theme) {
        Map<ThemeColorCategory, ColorCategory> categoryMap = new HashMap<>();
        ListMultimap<ColorCategory, Integer> colorMap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
        for (ThemeColor color : theme.getColors()) {
            ColorCategory category = categoryMap.computeIfAbsent(color.getCodeColor().getCategory(), k -> new ColorCategory(k.getName()));
            category.colors.add(new Color(color.getLabel(), getRgb(color.getRgba())));
        }
        this.categories = new ArrayList<>(categoryMap.values());
    }

    private static int getRgb(float[] rgba) {
        return ImColor.rgba(rgba[3], rgba[2], rgba[1], rgba[0]);
    }

    public List<ColorCategory> getCategories() {
        return categories;
    }

    @XStreamAlias("category")
    public static class ColorCategory {
        @XStreamAsAttribute
        private String name;

        @XStreamImplicit
        private List<Color> colors;

        public ColorCategory(String name) {
            this.name = name;
            this.colors = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public List<Color> getColors() {
            return colors;
        }
    }

    @XStreamAlias("color")
    public static class Color {
        @XStreamAsAttribute
        private String name;

        @XStreamAsAttribute
        private String value;

        public Color(String name, int color) {
            this.name = name;
            this.value = String.format("#%08X", color);
        }

        public int getColor() {
            try {
                return (int) Long.parseLong(value.substring(1), 16);
            } catch (Throwable throwable) {
                Logging.warn("Unable to parse color '{}': {}", this.getName(), throwable);
                return 0;
            }
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    public static String serialize(ThemeFileNew file) {
        return stream.toXML(file);
    }

    public static void deserialize(ThemeFileNew root, String data) {
        stream.fromXML(data, root);
    }

    static {
        stream.allowTypes(new Class[] {
                Color.class, ThemeFileNew.class, ColorCategory.class
        });
        stream.processAnnotations(ThemeFileNew.class);
        stream.useAttributeFor(Color.class, "name");
        stream.useAttributeFor(Color.class, "value");
    }
}
