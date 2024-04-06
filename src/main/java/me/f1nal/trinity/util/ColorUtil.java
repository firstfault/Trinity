package me.f1nal.trinity.util;

import java.awt.*;

public final class ColorUtil {
    public static Color darker(Color color, float factor) {
        return new Color(Math.max((int) (color.getRed() * factor), 0),
                Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0),
                color.getAlpha());
    }

    public static Color brighter(Color color, float factor) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();

        int i = (int) (1 / (1 - factor));
        if (red == 0 && green == 0 && blue == 0) {
            return new Color(i, i, i, alpha);
        }

        if (red > 0 && red < i) red = i;
        if (green > 0 && green < i) green = i;
        if (blue > 0 && blue < i) blue = i;

        return new Color(Math.min((int) (red / factor), 255),
                Math.min((int) (green / factor), 255),
                Math.min((int) (blue / factor), 255),
                alpha);
    }

    public static int brighter(int rgb, float factor) {
        int alpha = (rgb >> 24) & 0xFF;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        int i = (int) (1 / (1 - factor));
        if (red == 0 && green == 0 && blue == 0) {
            return (alpha << 24) | (i << 16) | (i << 8) | i;
        }

        if (red > 0 && red < i) red = i;
        if (green > 0 && green < i) green = i;
        if (blue > 0 && blue < i) blue = i;

        int newRed = Math.min((int) (red / factor), 255);
        int newGreen = Math.min((int) (green / factor), 255);
        int newBlue = Math.min((int) (blue / factor), 255);

        return (alpha << 24) | (newRed << 16) | (newGreen << 8) | newBlue;
    }

    public static Color colorPulse(Color color, Color secondColor, float offset) {
        return mixColors(color, secondColor, (float) Math.cos(System.currentTimeMillis() * -0.004 + offset * 0.1) * -0.3F + 0.65F);
    }

    public static Color mixColors(Color color, Color secondColor, double percent) {
        percent = Math.max(Math.min(percent, 1.D), 0.D);
        double inverse;
        if (percent > 1.D) {
            inverse = 0.D;
        } else {
            inverse = 1.0D - percent;
        }
        double r = (color.getRed() * percent) + (secondColor.getRed() * inverse);
        double g = (color.getGreen() * percent) + (secondColor.getGreen() * inverse);
        double b = (color.getBlue() * percent) + (secondColor.getBlue() * inverse);
        double a = (color.getAlpha() * percent) + (secondColor.getAlpha() * inverse);
        return new Color((int)r,(int)g,(int)b,(int)a);
    }

    public static Color changeAlpha(Color color, int target) {
        if (target == color.getAlpha()) {
            return color;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), fixRangeRgb(target));
    }

    public static int changeAlpha(int color, int target) {
        return (color & 0x00FFFFFF) | (target << 24);
    }

    public static int fixRangeRgb(double value) {
        return fixRangeRgb((int) value);
    }
}
