package me.f1nal.trinity.appdata.keybindings;

import java.util.Objects;

public class KeyBindingData {
    private final int keyCode;
    private final String shortName;
    private final boolean control;
    private final boolean shift;
    private final boolean alt;
    private final boolean superKey;

    public KeyBindingData(int keyCode, String shortName) {
        this(keyCode, shortName, false, false, false, false);
    }

    public KeyBindingData(int keyCode, String shortName, boolean control, boolean shift,
                          boolean alt, boolean superKey) {
        this.keyCode = keyCode;
        this.shortName = shortName;
        this.control = control;
        this.shift = shift;
        this.alt = alt;
        this.superKey = superKey;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isControl() {
        return control;
    }

    public boolean isShift() {
        return shift;
    }

    public boolean isAlt() {
        return alt;
    }

    public boolean isSuperKey() {
        return superKey;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof KeyBindingData data && Objects.equals(this.shortName, data.shortName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shortName);
    }
}
