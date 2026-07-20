package me.f1nal.trinity.theme;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccentThemeTest {
    @Test
    void primarySyntaxRolesRemainDistinctAndReadableForEveryAccent() {
        AccentTheme theme = AccentTheme.dark();

        for (AccentColor accent : AccentColor.values()) {
            theme.apply(accent);
            Set<Integer> roles = Set.of(
                    CodeColorScheme.METHOD_REF,
                    CodeColorScheme.FIELD_REF,
                    CodeColorScheme.CLASS_REF,
                    CodeColorScheme.KEYWORD,
                    CodeColorScheme.STRING,
                    CodeColorScheme.NUMBER);

            assertEquals(6, roles.size(), () -> "Collapsed syntax colors for " + accent.getName());
            for (int role : roles) {
                assertTrue(contrast(role, CodeColorScheme.BACKGROUND) >= 4.5,
                        () -> "Low editor contrast for " + accent.getName());
            }
        }
    }

    @Test
    void tintedAndDarkKeepSeparateSurfaceTreatments() {
        AccentTheme.tinted().apply(AccentColor.VIOLET);
        int tintedBackground = CodeColorScheme.BACKGROUND;

        AccentTheme.dark().apply(AccentColor.VIOLET);

        assertNotEquals(tintedBackground, CodeColorScheme.BACKGROUND);
    }

    @Test
    void primarySyntaxRolesFollowAccentWithoutLosingTheirStableBase() {
        AccentTheme theme = AccentTheme.dark();
        theme.apply(AccentColor.SAPPHIRE);
        int[] sapphire = primaryRoles();

        theme.apply(AccentColor.EMBER);
        int[] ember = primaryRoles();

        for (int index = 0; index < sapphire.length; index++) {
            double distance = colorDistance(sapphire[index], ember[index]);
            assertTrue(distance >= 30.0, "Syntax role only received a slight accent tint");
            assertTrue(distance <= 110.0, "Accent overwhelmed the syntax role's base color");
        }
    }

    @Test
    void ordinaryClassColorKeepsAccentHueWithSofterSaturation() {
        AccentTheme theme = AccentTheme.dark();

        for (AccentColor accent : AccentColor.values()) {
            theme.apply(accent);
            float[] selected = hsb(accent.getColor());
            float[] classColor = hsb(CodeColorScheme.CLASS_REF);
            assertEquals(selected[0], classColor[0], 0.002F);
            assertEquals(selected[1] * 0.82F, classColor[1], 0.01F);
            assertEquals(selected[2], classColor[2], 0.01F);
        }
    }

    private static float[] hsb(int packedColor) {
        Color color = CodeColorScheme.toColor(packedColor);
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    }

    private static int[] primaryRoles() {
        return new int[]{
                CodeColorScheme.METHOD_REF,
                CodeColorScheme.FIELD_REF,
                CodeColorScheme.KEYWORD,
                CodeColorScheme.STRING,
                CodeColorScheme.NUMBER
        };
    }

    private static double colorDistance(int first, int second) {
        Color a = CodeColorScheme.toColor(first);
        Color b = CodeColorScheme.toColor(second);
        int red = a.getRed() - b.getRed();
        int green = a.getGreen() - b.getGreen();
        int blue = a.getBlue() - b.getBlue();
        return Math.sqrt(red * red + green * green + blue * blue);
    }

    private static double contrast(int first, int second) {
        double bright = Math.max(luminance(first), luminance(second));
        double dark = Math.min(luminance(first), luminance(second));
        return (bright + 0.05) / (dark + 0.05);
    }

    private static double luminance(int packedColor) {
        Color color = CodeColorScheme.toColor(packedColor);
        return 0.2126 * linear(color.getRed())
                + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
