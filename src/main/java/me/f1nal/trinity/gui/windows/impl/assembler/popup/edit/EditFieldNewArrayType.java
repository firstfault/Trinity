package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import imgui.ImGui;
import imgui.type.ImString;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class EditFieldNewArrayType extends EditField<Integer> {
    private static final Map<String, Integer> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("boolean", Opcodes.T_BOOLEAN);
        TYPES.put("char", Opcodes.T_CHAR);
        TYPES.put("float", Opcodes.T_FLOAT);
        TYPES.put("double", Opcodes.T_DOUBLE);
        TYPES.put("byte", Opcodes.T_BYTE);
        TYPES.put("short", Opcodes.T_SHORT);
        TYPES.put("int", Opcodes.T_INT);
        TYPES.put("long", Opcodes.T_LONG);
    }

    private final ImString value = new ImString(16);
    private boolean valid = true;

    EditFieldNewArrayType(Supplier<Integer> getter, Consumer<Integer> setter) {
        super(getter, setter);
    }

    @Override
    public void draw() {
        if (ImGui.inputTextWithHint("Array type", "int", value)) this.applyInlineValue(value.get());
    }

    @Override
    public void updateField() {
        value.set(nameOf(this.get()));
    }

    @Override
    public boolean isValidInput() {
        return valid;
    }

    @Override
    public String getInlineLabel() {
        return "Array type";
    }

    @Override
    protected String formatInlineValue() {
        return nameOf(this.get());
    }

    @Override
    public boolean applyInlineValue(String input) {
        Integer type = TYPES.get(input.trim().toLowerCase(Locale.ROOT));
        this.valid = type != null;
        if (type == null) {
            this.update();
            return false;
        }
        this.value.set(input.toLowerCase(Locale.ROOT));
        this.set(type);
        return true;
    }

    @Override
    public List<String> getInlineSuggestions() {
        return List.copyOf(TYPES.keySet());
    }

    @Override
    public String getInlineError() {
        return valid ? null : "Expected a primitive array type";
    }

    private static String nameOf(int value) {
        return TYPES.entrySet().stream().filter(entry -> entry.getValue() == value)
                .map(Map.Entry::getKey).findFirst().orElse(Integer.toString(value));
    }
}
