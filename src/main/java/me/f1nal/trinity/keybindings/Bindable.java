package me.f1nal.trinity.keybindings;

import java.awt.event.KeyEvent;

public class Bindable {
    private final String identifier;
    private final String displayName;
    private int keyCode;
    private String keyName;

    public Bindable(String identifier, String displayName) {
        this.identifier = identifier;
        this.displayName = displayName;

        this.bind(-1);
    }

    public void bind(int keyCode) {
        this.keyCode = keyCode;
        this.keyName = keyCode == -1 ? "None" : KeyEvent.getKeyText(keyCode);
    }

    public boolean isBound() {
        return keyCode != -1;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }
}
