package me.f1nal.trinity.appdata.keybindings;

public class KeyBindingData {
    private final int keyCode;
    private final String shortName;

    public KeyBindingData(int keyCode, String shortName) {
        this.keyCode = keyCode;
        this.shortName = shortName;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getShortName() {
        return shortName;
    }
}
