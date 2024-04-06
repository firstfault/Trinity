package me.f1nal.trinity.appdata;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import me.f1nal.trinity.util.SerializationUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO for serializing/deserializing a theme from disk.
 */
public class ThemeFile {
    @XStreamOmitField
    private static final XStream stream = new XStream();
    private final Map<String, Integer> colors = new HashMap<>();

    public Map<String, Integer> getColors() {
        return colors;
    }

    public static String serialize(ThemeFile file) {
        return stream.toXML(file);
    }

    public static void deserialize(ThemeFile root, String data) {
        stream.fromXML(data, root);
    }

    static {
        SerializationUtil.addAlias(stream, ThemeFile.class, "theme");
    }
}
