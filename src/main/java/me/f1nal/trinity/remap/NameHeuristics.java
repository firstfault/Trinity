package me.f1nal.trinity.remap;

import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NameHeuristics {
    private static final List<String> WHITELISTED_SHORT_NAMES = List.of(
            "ok"
    );

    // TODO: need proper handling of classes & respective packages
    public boolean isNameObfuscated(@NotNull String name, InputType type) {
        if (type == InputType.CLASS) {
            name = NameUtil.getSimpleName(name);
        }

        final int stringLength = name.length();

        if (stringLength <= 2) {
            if (!WHITELISTED_SHORT_NAMES.contains(name.toLowerCase())) {
                return true;
            }
        }

        final char[] chars = name.toCharArray();

        if (stringLength >= 4) {
            if (isIlIName(chars)) {
                return true;
            }
        }

        if (isUnicodeName(chars)) {
            return true;
        }

        return isRandomSequence(chars);
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

        return uppercase >= lowercase / 3.F;
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
