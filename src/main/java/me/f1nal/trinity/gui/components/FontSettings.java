package me.f1nal.trinity.gui.components;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiSliderFlags;
import imgui.type.ImFloat;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.gui.components.general.EnumComboBox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FontSettings {
    private FontEnum font;
    private float size;
    /**
     * Size that the font was built with.
     */
    @XStreamOmitField
    private float builtSize;
    @XStreamOmitField
    private final EnumComboBox<FontEnum> comboBox;
    @XStreamOmitField
    private Map<FontEnum, ImFont> imFontMap;
    @XStreamOmitField
    private ImFont imFont;
    @XStreamOmitField
    private ImFont iconFont;

    public FontSettings(FontEnum font, float size, String label) {
        this.font = font;
        this.size = size;
        this.imFontMap = new HashMap<>();
        this.comboBox = new EnumComboBox<>(label + " Font", FontEnum.values(), this.font);
    }

    public void setIconFont(ImFont iconFont) {
        this.iconFont = iconFont;
    }

    public ImFont getIconFont() {
        return iconFont;
    }

    public void registerFont(FontEnum font, ImFont imFont) {
        this.imFontMap.put(font, imFont);

        if (this.font == font) {
            this.setFont(font);
        }
    }

    public float getBuiltSize() {
        return builtSize;
    }

    public void setBuiltSize(float builtSize) {
        this.builtSize = builtSize;
    }

    /**
     * Scale between the size it was built with and newly requested size. Usually {@code 1} unless font size was changed during this Trinity session.
     */
    public float getGlobalScale() {
        return this.getSize() / this.getBuiltSize();
    }

    public FontEnum getFont() {
        return font;
    }

    public void setFont(FontEnum font) {
        this.font = font;
        this.imFont = this.imFontMap.get(font);
    }

    public ImFont getImFont() {
        return imFont;
    }

    public float getSize() {
        return Math.min(Math.max(this.size, 12.F), 30.F);
    }

    public void setSize(float size) {
        this.size = size;
    }

    public void drawControls() {
        this.setFont(this.comboBox.draw());
        ImFloat fontSize = new ImFloat(this.getSize());
        ImGui.inputScalar("Font Size", ImGuiDataType.Float, fontSize, 0.5F, 1.F, "%.2f px", ImGuiSliderFlags.AlwaysClamp);
        this.setSize(fontSize.get());

        if (this.getBuiltSize() != this.getSize()) {
            ImGui.textDisabled("Please restart Trinity for a clear version of the font.");
        }
    }

    public void pushFont() {
        this.imFont.setScale(this.getGlobalScale());
        ImGui.pushFont(this.imFont);
    }

    public void popFont() {
        ImGui.popFont();
    }

    public static class FontSettingsConverter implements Converter {
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            FontSettings fontSettings = (FontSettings) source;

            writer.addAttribute("font", fontSettings.getFont().getName());
            writer.addAttribute("size", String.valueOf(fontSettings.getSize()));
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return new FontSettings(FontEnum.getFont(reader.getAttribute("font")),
                    Float.parseFloat(reader.getAttribute("size")),
                    reader.getNodeName().contains("decompiler") ? "Decompiler" : "Default"); // not too great
        }

        @Override
        public boolean canConvert(Class type) {
            return type == FontSettings.class;
        }
    }
}
