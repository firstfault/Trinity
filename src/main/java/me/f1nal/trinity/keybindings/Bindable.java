package me.f1nal.trinity.keybindings;

import imgui.ImGui;
import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** A registered action with a stable identifier and a configurable keyboard chord. */
public final class Bindable {
    private final String identifier;
    private final String category;
    private final String scope;
    private final String displayName;
    private final KeyChord defaultChord;
    private KeyChord chord;

    Bindable(String identifier, String category, String scope, String displayName, KeyChord defaultChord) {
        this.identifier = identifier;
        this.category = category;
        this.scope = scope;
        this.displayName = displayName;
        this.defaultChord = defaultChord;
        this.chord = defaultChord;
    }

    public void bind(int keyCode, boolean control, boolean shift, boolean alt, boolean superKey) {
        this.chord = new KeyChord(keyCode, control, shift, alt, superKey);
    }

    public void bind(KeyBindingData data) {
        this.bind(data.getKeyCode(), data.isControl(), data.isShift(), data.isAlt(), data.isSuperKey());
    }

    public void clear() {
        this.bind(-1, false, false, false, false);
    }

    public void reset() {
        this.chord = this.defaultChord;
    }

    public boolean isPressed() {
        if (!this.isBound()) return false;
        return ImGui.getIO().getKeyCtrl() == chord.control()
                && ImGui.getIO().getKeyShift() == chord.shift()
                && ImGui.getIO().getKeyAlt() == chord.alt()
                && ImGui.getIO().getKeySuper() == chord.superKey()
                && ImGui.isKeyPressed(chord.keyCode());
    }

    public boolean isBound() {
        return chord.keyCode() != -1;
    }

    public boolean isDefault() {
        return chord.equals(defaultChord);
    }

    public boolean hasChord(int keyCode, boolean control, boolean shift, boolean alt, boolean superKey) {
        return this.chord.equals(new KeyChord(keyCode, control, shift, alt, superKey));
    }

    public int getKeyCode() {
        return chord.keyCode();
    }

    public boolean isControl() {
        return chord.control();
    }

    public boolean isShift() {
        return chord.shift();
    }

    public boolean isAlt() {
        return chord.alt();
    }

    public boolean isSuperKey() {
        return chord.superKey();
    }

    public String getKeyName() {
        if (!this.isBound()) return "None";
        List<String> parts = new ArrayList<>(5);
        if (chord.control()) parts.add("Ctrl");
        if (chord.shift()) parts.add("Shift");
        if (chord.alt()) parts.add("Alt");
        if (chord.superKey()) parts.add("Super");
        parts.add(getKeyName(chord.keyCode()));
        return String.join("+", parts);
    }

    public KeyBindingData createData() {
        return new KeyBindingData(chord.keyCode(), identifier, chord.control(), chord.shift(),
                chord.alt(), chord.superKey());
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCategory() {
        return category;
    }

    public String getScope() {
        return scope;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static boolean isModifierKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
                || keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static String getKeyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return Character.toString((char) keyCode);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return Character.toString((char) keyCode);
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_ESCAPE -> "Escape";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_PAGE_UP -> "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll Lock";
            case GLFW.GLFW_KEY_NUM_LOCK -> "Num Lock";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen";
            case GLFW.GLFW_KEY_PAUSE -> "Pause";
            default -> {
                String name = GLFW.glfwGetKeyName(keyCode, 0);
                yield name == null ? "Key " + keyCode : name.toUpperCase();
            }
        };
    }

    record KeyChord(int keyCode, boolean control, boolean shift, boolean alt, boolean superKey) {
    }
}
