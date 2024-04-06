package me.f1nal.trinity.decompiler.output.colors;

import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ColoredStringBuilder {
    private final List<ColoredString> strings = new ArrayList<>();

    private ColoredStringBuilder() {

    }

    public List<ColoredString> get() {
        return this.strings;
    }

    public ColoredStringBuilder newline() {
        return add(new ColoredString("\n", 0));
    }

    public ColoredStringBuilder text(int color, String text) {
        return add(new ColoredString(text, color));
    }

    public ColoredStringBuilder fmt(String format, Object... args) {
        return fmt(CodeColorScheme.DISABLED, CodeColorScheme.TEXT, format, args);
    }

    public ColoredStringBuilder fmt(int txt, int hgh, String format, Object... args) {
        format = format.replace("%n", "\n");
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                String text;
                if (argIndex < args.length) {
                    text = Objects.toString(args[argIndex]);
                    argIndex++;
                } else {
                    text = "{}";
                }
                if (!sb.isEmpty()) {
                    this.text(txt, sb.toString());
                    sb = new StringBuilder();
                }
                this.text(hgh, text);
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        if (!sb.isEmpty()) this.text(txt, sb.toString());
        return this;
    }

    public boolean isEmpty() {
        return strings.isEmpty();
    }

    private ColoredStringBuilder add(ColoredString string) {
        strings.add(string);
        return this;
    }

    public static ColoredStringBuilder create() {
        return new ColoredStringBuilder();
    }
}
