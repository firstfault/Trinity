package me.f1nal.trinity.theme;

import java.awt.Color;

/** A built-in editor theme harmonized with the selected accent color. */
public final class AccentTheme extends Theme {
    private static final Color BASE_BACKGROUND = new Color(23, 25, 30);
    private static final Color BASE_POPUP = new Color(29, 32, 38);
    private static final Color BASE_HIGHLIGHT = new Color(18, 20, 24);
    private static final Color BASE_WIDGET = new Color(42, 46, 55);

    private final float backgroundTintScale;
    private Color activeBackground = BASE_BACKGROUND;

    private AccentTheme(String name, float backgroundTintScale) {
        super(name, false);
        this.backgroundTintScale = backgroundTintScale;
    }

    public static AccentTheme tinted() {
        return new AccentTheme("Accent (Tinted)", 1.F);
    }

    public static AccentTheme dark() {
        return new AccentTheme("Accent (Darker)", 0.14F);
    }

    @Override
    public boolean isVisibleInEditor() {
        return false;
    }

    @Override
    public boolean isAccentAdaptive() {
        return true;
    }

    @Override
    public void apply(AccentColor accentColor) {
        AccentColor selected = accentColor == null ? AccentColor.SAPPHIRE : accentColor;
        Color accent = CodeColorScheme.toColor(selected.getColor());
        this.activeBackground = background(BASE_BACKGROUND, accent, 0.055F);

        for (CodeColor codeColor : CodeColorScheme.getCodeColors()) {
            codeColor.setColor(this.createColor(codeColor.getField().getName(), accent));
        }

        // These aliases are intentionally not theme-editor colors, but must follow their source colors.
        CodeColorScheme.XREF_TYPE = CodeColorScheme.CLASS_REF;
        CodeColorScheme.XREF_INVOKE = CodeColorScheme.METHOD_REF;
        CodeColorScheme.XREF_FIELD = CodeColorScheme.FIELD_REF;
    }

    private int createColor(String name, Color accent) {
        return switch (name) {
            // Status colors keep conventional meaning. The accent only ties them into the palette.
            case "NOTIFY_ERROR" -> semantic(new Color(238, 102, 112), accent, 0.06F);
            case "NOTIFY_WARN" -> semantic(new Color(229, 192, 123), accent, 0.06F);
            case "NOTIFY_INFORMATION" -> semantic(new Color(105, 175, 235), accent, 0.09F);
            case "NOTIFY_SUCCESS" -> semantic(new Color(126, 198, 153), accent, 0.06F);

            // A clear luminance ladder separates the editor, popups, and controls at a glance.
            case "HIGHLIGHT_BACKGROUND" -> rgba(background(BASE_HIGHLIGHT, accent, 0.075F), 255);
            case "BACKGROUND" -> rgba(background(BASE_BACKGROUND, accent, 0.055F), 255);
            case "POPUP_BACKGROUND" -> rgba(background(BASE_POPUP, accent, 0.07F), 255);
            case "WIDGET_BACKGROUND" -> rgba(background(BASE_WIDGET, accent, 0.085F), 220);

            // File kinds retain familiar editor colors while visibly following the accent.
            case "FILE_CLASS" -> rgba(classAccent(accent), 235);
            case "FILE_INTERFACE" -> adaptiveTone(new Color(86, 196, 214), accent,
                    164.F, 0.50F, 0.90F, 0.28F, 255);
            case "FILE_ABSTRACT" -> adaptiveTone(new Color(202, 166, 242), accent,
                    -48.F, 0.48F, 0.92F, 0.28F, 255);
            case "FILE_ENUM" -> adaptiveTone(new Color(235, 180, 115), accent,
                    -118.F, 0.56F, 0.94F, 0.26F, 255);
            case "FILE_RESOURCE" -> adaptiveTone(new Color(218, 122, 136), accent,
                    178.F, 0.48F, 0.88F, 0.26F, 255);
            case "FILE_ANNOTATION" -> adaptiveTone(new Color(215, 186, 125), accent,
                    -88.F, 0.52F, 0.92F, 0.26F, 255);

            // Stable base colors make roles recognizable; the accent-relative component is
            // strong enough to change the character of the palette without dominating it.
            case "METHOD_REF" -> adaptiveTone(new Color(220, 220, 170), accent,
                    0.F, 0.58F, 0.94F, 0.34F, 255);
            case "FIELD_REF" -> adaptiveTone(new Color(144, 201, 239), accent,
                    58.F, 0.55F, 0.92F, 0.32F, 255);
            case "CLASS_REF" -> rgba(classAccent(accent), 255);
            case "CLASS_REF_INTERFACE" -> adaptiveTone(new Color(86, 196, 214), accent,
                    164.F, 0.50F, 0.90F, 0.28F, 255);
            case "CLASS_REF_ABSTRACT" -> adaptiveTone(new Color(202, 166, 242), accent,
                    -48.F, 0.48F, 0.92F, 0.28F, 255);
            case "CLASS_REF_ENUM" -> adaptiveTone(new Color(235, 180, 115), accent,
                    -118.F, 0.56F, 0.94F, 0.26F, 255);
            case "CLASS_REF_ANNOTATION" -> adaptiveTone(new Color(215, 186, 125), accent,
                    -88.F, 0.52F, 0.92F, 0.26F, 255);
            case "VAR_REF" -> adaptiveTone(new Color(205, 211, 221), accent,
                    16.F, 0.20F, 0.88F, 0.16F, 235);
            case "PARAM_REF" -> adaptiveTone(new Color(177, 163, 224), accent,
                    30.F, 0.48F, 0.94F, 0.30F, 245);
            case "ARCHIVE_REF" -> adaptiveTone(new Color(178, 157, 219), accent,
                    -32.F, 0.34F, 0.78F, 0.22F, 255);
            case "KEYWORD" -> adaptiveTone(new Color(197, 134, 192), accent,
                    -62.F, 0.50F, 0.92F, 0.30F, 255);
            case "LABEL" -> syntax(new Color(179, 186, 197), accent, 0.04F, 255);
            case "NUMBER" -> adaptiveTone(new Color(181, 206, 168), accent,
                    -132.F, 0.44F, 0.91F, 0.27F, 255);
            case "DISABLED" -> subdued(new Color(121, 127, 139), accent, 0.04F);
            case "TEXT" -> syntax(new Color(212, 215, 221), accent, 0.035F, 255);
            case "PACKAGE" -> adaptiveTone(new Color(160, 169, 184), accent,
                    112.F, 0.26F, 0.76F, 0.18F, 255);
            case "STRING" -> adaptiveTone(new Color(206, 145, 120), accent,
                    178.F, 0.46F, 0.88F, 0.27F, 255);
            case "LINE_NUMBER" -> subdued(new Color(101, 108, 120), accent, 0.06F);
            case "CURSOR" -> syntax(new Color(232, 235, 240), accent, 0.08F, 255);
            case "CURSOR_SELECTION" -> withAlpha(ensureReadable(accent, 3.0), 78);
            case "SEARCH_RESULT" -> withAlpha(adaptiveToneColor(new Color(229, 192, 123),
                    accent, -118.F, 0.58F, 0.94F, 0.24F), 112);

            // Assembly families remain unmistakable even in dense instruction streams.
            case "KEYWORD_DATA" -> syntax(new Color(97, 175, 239), accent, 0.08F, 255);
            case "KEYWORD_JUMP" -> syntax(new Color(229, 192, 123), accent, 0.06F, 255);
            case "KEYWORD_CALL" -> syntax(new Color(152, 195, 121), accent, 0.06F, 255);

            case "XREF_INHERIT" -> syntax(new Color(86, 196, 214), accent, 0.07F, 255);
            case "XREF_RETURN" -> syntax(new Color(152, 195, 121), accent, 0.06F, 255);
            case "XREF_PARAMETER" -> syntax(new Color(177, 163, 224), accent, 0.08F, 255);
            case "XREF_LITERAL" -> syntax(new Color(206, 145, 120), accent, 0.05F, 255);
            case "XREF_EXCEPTION" -> syntax(new Color(238, 102, 112), accent, 0.05F, 255);
            case "XREF_ANNOTATION" -> syntax(new Color(215, 186, 125), accent, 0.06F, 255);
            default -> syntax(accent, accent, 0.F, 255);
        };
    }

    private Color background(Color base, Color accent, float tint) {
        return mix(base, accent, tint * this.backgroundTintScale);
    }

    private int semantic(Color base, Color accent, float accentAmount) {
        return rgba(ensureReadable(mix(base, accent, accentAmount), 4.5), 255);
    }

    private int syntax(Color base, Color accent, float accentAmount, int alpha) {
        return rgba(syntaxColor(base, accent, accentAmount), alpha);
    }

    private int adaptiveTone(Color base, Color accent, float hueOffset, float saturation,
                             float brightness, float accentAmount, int alpha) {
        return rgba(adaptiveToneColor(base, accent, hueOffset, saturation, brightness, accentAmount), alpha);
    }

    private Color adaptiveToneColor(Color base, Color accent, float hueOffset, float saturation,
                                    float brightness, float accentAmount) {
        return ensureReadable(mix(base, tone(accent, hueOffset, saturation, brightness), accentAmount), 4.5);
    }

    private Color tone(Color accent, float hueOffset, float saturation, float brightness) {
        float[] selected = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        float hue = (selected[0] + hueOffset / 360.F) % 1.F;
        if (hue < 0.F) {
            hue += 1.F;
        }
        float adaptedSaturation = clamp(saturation * 0.72F + selected[1] * 0.28F);
        return ensureReadable(Color.getHSBColor(hue, adaptedSaturation, clamp(brightness)), 4.5);
    }

    private static Color classAccent(Color accent) {
        float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * 0.82F, hsb[2]);
    }

    private Color syntaxColor(Color base, Color accent, float accentAmount) {
        return ensureReadable(mix(base, accent, accentAmount), 4.5);
    }

    private int subdued(Color base, Color accent, float accentAmount) {
        return rgba(ensureReadable(mix(base, accent, accentAmount), 3.0), 255);
    }

    private Color ensureReadable(Color foreground, double minimumContrast) {
        Color adjusted = foreground;
        for (int step = 1; contrastRatio(adjusted, this.activeBackground) < minimumContrast && step <= 12; step++) {
            adjusted = mix(foreground, Color.WHITE, step * 0.04F);
        }
        return adjusted;
    }

    private static double contrastRatio(Color first, Color second) {
        double bright = Math.max(relativeLuminance(first), relativeLuminance(second));
        double dark = Math.min(relativeLuminance(first), relativeLuminance(second));
        return (bright + 0.05) / (dark + 0.05);
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linear(color.getRed())
                + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static Color mix(Color first, Color second, float amount) {
        float bounded = clamp(amount);
        int red = Math.round(first.getRed() + (second.getRed() - first.getRed()) * bounded);
        int green = Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * bounded);
        int blue = Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * bounded);
        return new Color(red, green, blue);
    }

    private static int rgba(Color color, int alpha) {
        return CodeColorScheme.getRgb(color, alpha);
    }

    private static int withAlpha(Color color, int alpha) {
        return rgba(color, alpha);
    }

    private static float clamp(float value) {
        return Math.max(0.F, Math.min(1.F, value));
    }

}
