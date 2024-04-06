package me.f1nal.trinity.theme;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class ThemeColor {
    private final String label;
    private final float[] rgba;
    @XStreamOmitField
    private final CodeColor codeColor;

    public ThemeColor(String label, int rgba, CodeColor codeColor) {
        this.label = label;

        this.rgba = CodeColorScheme.toRgba(rgba);
        this.codeColor = codeColor;
    }

    public String getLabel() {
        return label;
    }

    public float[] getRgba() {
        return rgba;
    }

    public void setRgba(float[] rgba) {
        System.arraycopy(rgba, 0, this.getRgba(), 0, rgba.length);
    }

    public CodeColor getCodeColor() {
        return codeColor;
    }
}
