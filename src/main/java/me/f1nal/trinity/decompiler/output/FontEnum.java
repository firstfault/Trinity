package me.f1nal.trinity.decompiler.output;

import imgui.ImFont;
import me.f1nal.trinity.util.INameable;

import java.util.Arrays;
import java.util.Objects;

public enum FontEnum implements INameable {
    JETBRAINS_MONO("JetBrains Mono"),
    ZED_MONO("Zed Mono"),
    INTER("Inter"),
    ;

    private final String name;

    FontEnum(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public static FontEnum getFont(String name) {
        return Objects.requireNonNull(Arrays.stream(values()).filter(value -> value.getName().equals(name)).findFirst().orElse(null), String.format("Font type '%s'", name));
    }
}
