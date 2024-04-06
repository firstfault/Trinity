package me.f1nal.trinity.keybindings;

public class KeyBinding {
    private final String shortName;
    private final int keyCode;

    public KeyBinding(String shortName, int keyCode) {
        this.shortName = shortName;
        this.keyCode = keyCode;
    }

    public String getShortName() {
        return shortName;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
