package me.f1nal.trinity.remap;

import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NameHeuristics {
    private static final List<String> WHITELISTED_SHORT_NAMES = List.of(
            "ok", "io", "co", "gg"
    );
    private final MemorableCheckboxComponent enabledCheckbox = new MemorableCheckboxComponent("nameHeuristicsEnabled", "Name Heuristics", true);

    public boolean isObfuscated(@NotNull String name, InputType type) {
        if (!enabledCheckbox.isChecked()) {
            return true;
        }

        final char[] chars = name.toCharArray();
        final int length = chars.length;

        if (type == InputType.PACKAGE) {
            if (length <= 1) {
                return true;
            }
        }
        if (length <= 2 && WHITELISTED_SHORT_NAMES.contains(name.toLowerCase())) {
            return true;
        }

        if (length >= 3 && isIlIName(chars)) {
            return true;
        }

        if (isUnicodeName(chars)) {
            return true;
        }

        if (isRandomSequence(chars)) {
            return true;
        }

        NameUtil.TextAnalysisResult analysis = NameUtil.getWordAnalysis(name.toLowerCase());
        return ((float)analysis.getUnrecognizedCharacters() * 0.87F) * (Math.max(chars.length / 10.F, 1.F)) > (float)analysis.getRecognizedCharacters() * 2.F;
    }

    public MemorableCheckboxComponent getEnabledCheckbox() {
        return enabledCheckbox;
    }

    private static boolean isRandomSequence(char[] chars) {
        if (chars.length < 4) {
            return false;
        }

        float lowercase = 0.F;
        float uppercase = 0.F;
        float nonLetters = 0.F;

        for (char c : chars) {
            if (!Character.isLetter(c)) {
                ++nonLetters;
                continue;
            }

            if (Character.isUpperCase(c)) {
                ++uppercase;
            } else {
                ++lowercase;
            }
        }

        if (nonLetters > chars.length / 2.5F) {
            return true;
        }

        return lowercase > 3 && uppercase >= lowercase / 3.F;
    }

    private static boolean isUnicodeName(char[] chars) {
        for (char c : chars) {
            if (c > 127 || c <= 32) {
                return true;
            }
        }

        return false;
    }

    private static boolean isIlIName(char[] chars) {
        for (char c : chars) {
            c = Character.toLowerCase(c);

            if (c != 'i' && c != 'l') {
                return false;
            }
        }

        return true;
    }
}
