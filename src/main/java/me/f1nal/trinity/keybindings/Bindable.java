package me.f1nal.trinity.keybindings;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** A registered action with a stable identifier and a configurable keyboard chord. */
public final class Bindable {
    private static final int MOUSE_BUTTON_CODE_BASE = -1000;
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
        this.chord = new KeyChord(normalizeKeyCode(keyCode), control, shift, alt, superKey);
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
                && (isMouseButtonCode(chord.keyCode())
                ? ImGui.isMouseClicked(getMouseButton(chord.keyCode()))
                : ImGui.isKeyPressed(chord.keyCode()));
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
        int normalizedKeyCode = normalizeKeyCode(keyCode);
        return normalizedKeyCode == ImGuiKey.LeftCtrl || normalizedKeyCode == ImGuiKey.RightCtrl
                || normalizedKeyCode == ImGuiKey.LeftShift || normalizedKeyCode == ImGuiKey.RightShift
                || normalizedKeyCode == ImGuiKey.LeftAlt || normalizedKeyCode == ImGuiKey.RightAlt
                || normalizedKeyCode == ImGuiKey.LeftSuper || normalizedKeyCode == ImGuiKey.RightSuper;
    }

    private static String getKeyName(int keyCode) {
        if (isMouseButtonCode(keyCode)) {
            return "Mouse " + (getMouseButton(keyCode) + 1);
        }
        if (keyCode >= ImGuiKey._0 && keyCode <= ImGuiKey._9) {
            return Integer.toString(keyCode - ImGuiKey._0);
        }
        if (keyCode >= ImGuiKey.A && keyCode <= ImGuiKey.Z) {
            return Character.toString((char) ('A' + keyCode - ImGuiKey.A));
        }
        if (keyCode >= ImGuiKey.F1 && keyCode <= ImGuiKey.F24) {
            return "F" + (keyCode - ImGuiKey.F1 + 1);
        }
        if (keyCode >= ImGuiKey.Keypad0 && keyCode <= ImGuiKey.Keypad9) {
            return "Keypad " + (keyCode - ImGuiKey.Keypad0);
        }
        return switch (keyCode) {
            case ImGuiKey.Tab -> "Tab";
            case ImGuiKey.LeftArrow -> "Left";
            case ImGuiKey.RightArrow -> "Right";
            case ImGuiKey.UpArrow -> "Up";
            case ImGuiKey.DownArrow -> "Down";
            case ImGuiKey.PageUp -> "Page Up";
            case ImGuiKey.PageDown -> "Page Down";
            case ImGuiKey.Home -> "Home";
            case ImGuiKey.End -> "End";
            case ImGuiKey.Insert -> "Insert";
            case ImGuiKey.Delete -> "Delete";
            case ImGuiKey.Backspace -> "Backspace";
            case ImGuiKey.Space -> "Space";
            case ImGuiKey.Enter -> "Enter";
            case ImGuiKey.Escape -> "Escape";
            case ImGuiKey.LeftCtrl -> "Left Ctrl";
            case ImGuiKey.LeftShift -> "Left Shift";
            case ImGuiKey.LeftAlt -> "Left Alt";
            case ImGuiKey.LeftSuper -> "Left Super";
            case ImGuiKey.RightCtrl -> "Right Ctrl";
            case ImGuiKey.RightShift -> "Right Shift";
            case ImGuiKey.RightAlt -> "Right Alt";
            case ImGuiKey.RightSuper -> "Right Super";
            case ImGuiKey.Menu -> "Menu";
            case ImGuiKey.Apostrophe -> "'";
            case ImGuiKey.Comma -> ",";
            case ImGuiKey.Minus -> "-";
            case ImGuiKey.Period -> ".";
            case ImGuiKey.Slash -> "/";
            case ImGuiKey.Semicolon -> ";";
            case ImGuiKey.Equal -> "=";
            case ImGuiKey.LeftBracket -> "[";
            case ImGuiKey.Backslash -> "\\";
            case ImGuiKey.RightBracket -> "]";
            case ImGuiKey.GraveAccent -> "`";
            case ImGuiKey.CapsLock -> "Caps Lock";
            case ImGuiKey.ScrollLock -> "Scroll Lock";
            case ImGuiKey.NumLock -> "Num Lock";
            case ImGuiKey.PrintScreen -> "Print Screen";
            case ImGuiKey.Pause -> "Pause";
            case ImGuiKey.KeypadDecimal -> "Keypad .";
            case ImGuiKey.KeypadDivide -> "Keypad /";
            case ImGuiKey.KeypadMultiply -> "Keypad *";
            case ImGuiKey.KeypadSubtract -> "Keypad -";
            case ImGuiKey.KeypadAdd -> "Keypad +";
            case ImGuiKey.KeypadEnter -> "Keypad Enter";
            case ImGuiKey.KeypadEqual -> "Keypad =";
            case ImGuiKey.Oem102 -> "OEM 102";
            default -> "Key " + keyCode;
        };
    }

    /** Converts key codes saved by the pre-1.92 GLFW-based binding system to named ImGui keys. */
    static int normalizeKeyCode(int keyCode) {
        if (keyCode == -1) return -1;
        if (isMouseButtonCode(keyCode)) return keyCode;
        if (keyCode >= ImGuiKey.NamedKey_BEGIN && keyCode < ImGuiKey.NamedKey_END) return keyCode;
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return ImGuiKey._0 + keyCode - GLFW.GLFW_KEY_0;
        }
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return ImGuiKey.A + keyCode - GLFW.GLFW_KEY_A;
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F24) {
            return ImGuiKey.F1 + keyCode - GLFW.GLFW_KEY_F1;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return ImGuiKey.Keypad0 + keyCode - GLFW.GLFW_KEY_KP_0;
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> ImGuiKey.Tab;
            case GLFW.GLFW_KEY_LEFT -> ImGuiKey.LeftArrow;
            case GLFW.GLFW_KEY_RIGHT -> ImGuiKey.RightArrow;
            case GLFW.GLFW_KEY_UP -> ImGuiKey.UpArrow;
            case GLFW.GLFW_KEY_DOWN -> ImGuiKey.DownArrow;
            case GLFW.GLFW_KEY_PAGE_UP -> ImGuiKey.PageUp;
            case GLFW.GLFW_KEY_PAGE_DOWN -> ImGuiKey.PageDown;
            case GLFW.GLFW_KEY_HOME -> ImGuiKey.Home;
            case GLFW.GLFW_KEY_END -> ImGuiKey.End;
            case GLFW.GLFW_KEY_INSERT -> ImGuiKey.Insert;
            case GLFW.GLFW_KEY_DELETE -> ImGuiKey.Delete;
            case GLFW.GLFW_KEY_BACKSPACE -> ImGuiKey.Backspace;
            case GLFW.GLFW_KEY_SPACE -> ImGuiKey.Space;
            case GLFW.GLFW_KEY_ENTER -> ImGuiKey.Enter;
            case GLFW.GLFW_KEY_ESCAPE -> ImGuiKey.Escape;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> ImGuiKey.LeftCtrl;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> ImGuiKey.LeftShift;
            case GLFW.GLFW_KEY_LEFT_ALT -> ImGuiKey.LeftAlt;
            case GLFW.GLFW_KEY_LEFT_SUPER -> ImGuiKey.LeftSuper;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> ImGuiKey.RightCtrl;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> ImGuiKey.RightShift;
            case GLFW.GLFW_KEY_RIGHT_ALT -> ImGuiKey.RightAlt;
            case GLFW.GLFW_KEY_RIGHT_SUPER -> ImGuiKey.RightSuper;
            case GLFW.GLFW_KEY_MENU -> ImGuiKey.Menu;
            case GLFW.GLFW_KEY_APOSTROPHE -> ImGuiKey.Apostrophe;
            case GLFW.GLFW_KEY_COMMA -> ImGuiKey.Comma;
            case GLFW.GLFW_KEY_MINUS -> ImGuiKey.Minus;
            case GLFW.GLFW_KEY_PERIOD -> ImGuiKey.Period;
            case GLFW.GLFW_KEY_SLASH -> ImGuiKey.Slash;
            case GLFW.GLFW_KEY_SEMICOLON -> ImGuiKey.Semicolon;
            case GLFW.GLFW_KEY_EQUAL -> ImGuiKey.Equal;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> ImGuiKey.LeftBracket;
            case GLFW.GLFW_KEY_BACKSLASH -> ImGuiKey.Backslash;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> ImGuiKey.RightBracket;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> ImGuiKey.GraveAccent;
            case GLFW.GLFW_KEY_CAPS_LOCK -> ImGuiKey.CapsLock;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> ImGuiKey.ScrollLock;
            case GLFW.GLFW_KEY_NUM_LOCK -> ImGuiKey.NumLock;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> ImGuiKey.PrintScreen;
            case GLFW.GLFW_KEY_PAUSE -> ImGuiKey.Pause;
            case GLFW.GLFW_KEY_KP_DECIMAL -> ImGuiKey.KeypadDecimal;
            case GLFW.GLFW_KEY_KP_DIVIDE -> ImGuiKey.KeypadDivide;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> ImGuiKey.KeypadMultiply;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> ImGuiKey.KeypadSubtract;
            case GLFW.GLFW_KEY_KP_ADD -> ImGuiKey.KeypadAdd;
            case GLFW.GLFW_KEY_KP_ENTER -> ImGuiKey.KeypadEnter;
            case GLFW.GLFW_KEY_KP_EQUAL -> ImGuiKey.KeypadEqual;
            case GLFW.GLFW_KEY_WORLD_1 -> ImGuiKey.Oem102;
            default -> -1;
        };
    }

    public static int mouseButtonCode(int mouseButton) {
        if (mouseButton < 0 || mouseButton >= ImGuiMouseButton.COUNT) {
            throw new IllegalArgumentException("Unsupported mouse button: " + mouseButton);
        }
        return MOUSE_BUTTON_CODE_BASE - mouseButton;
    }

    public static boolean isMouseButtonCode(int code) {
        return code <= MOUSE_BUTTON_CODE_BASE
                && code > MOUSE_BUTTON_CODE_BASE - ImGuiMouseButton.COUNT;
    }

    public static int getMouseButton(int code) {
        if (!isMouseButtonCode(code)) {
            throw new IllegalArgumentException("Not a mouse binding: " + code);
        }
        return MOUSE_BUTTON_CODE_BASE - code;
    }

    record KeyChord(int keyCode, boolean control, boolean shift, boolean alt, boolean superKey) {
    }
}
