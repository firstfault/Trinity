package me.f1nal.trinity.keybindings;

import me.f1nal.trinity.appdata.keybindings.KeyBindingData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Registry for bindable actions. Definitions live here; only user overrides are persisted. */
public final class KeyBindManager {
    private static final String ASSEMBLER_INSTRUCTION_SCOPE = "assembler.instruction";
    private final Map<String, Bindable> bindables = new LinkedHashMap<>();

    public final Bindable ASSEMBLER_INSERT = register("assembler.instruction.insert", "Assembler",
            ASSEMBLER_INSTRUCTION_SCOPE, "Insert Instruction", GLFW.GLFW_KEY_A);
    public final Bindable ASSEMBLER_EDIT = register("assembler.instruction.edit", "Assembler",
            ASSEMBLER_INSTRUCTION_SCOPE, "Edit Instruction", GLFW.GLFW_KEY_E);
    public final Bindable ASSEMBLER_DELETE = register("assembler.instruction.delete", "Assembler",
            ASSEMBLER_INSTRUCTION_SCOPE, "Delete Instruction", GLFW.GLFW_KEY_X);
    public final Bindable ASSEMBLER_DUPLICATE = register("assembler.instruction.duplicate", "Assembler",
            ASSEMBLER_INSTRUCTION_SCOPE, "Duplicate Instruction", GLFW.GLFW_KEY_D);
    public final Bindable DECOMPILER_ASSEMBLE = register("decompiler.member.assemble", "Decompiler",
            "decompiler.member", "Assemble Method", GLFW.GLFW_KEY_A);
    public final Bindable DECOMPILER_RENAME = register("decompiler.member.rename", "Decompiler",
            "decompiler.member", "Rename Member", GLFW.GLFW_KEY_R);
    public final Bindable DECOMPILER_EDIT = register("decompiler.member.edit", "Decompiler",
            "decompiler.member", "Edit Field / Method / Class", GLFW.GLFW_KEY_E);
    public final Bindable DECOMPILER_VIEW_XREFS = register("decompiler.member.view_xrefs", "Decompiler",
            "decompiler.member", "View Xrefs", GLFW.GLFW_KEY_X);
    public final Bindable DECOMPILER_VIEW_MEMBER = register("decompiler.member.view", "Decompiler",
            "decompiler.member", "View Member", GLFW.GLFW_KEY_V);

    public Bindable register(String identifier, String category, String scope, String displayName, int defaultKey) {
        if (bindables.containsKey(identifier)) {
            throw new IllegalArgumentException("Duplicate key binding identifier: " + identifier);
        }
        Bindable bindable = new Bindable(identifier, category, scope, displayName,
                new Bindable.KeyChord(defaultKey, false, false, false, false));
        bindables.put(identifier, bindable);
        return bindable;
    }

    public void load(Set<KeyBindingData> storedBindings) {
        bindables.values().forEach(Bindable::reset);
        Map<String, KeyBindingData> byIdentifier = new HashMap<>();
        for (KeyBindingData data : storedBindings) byIdentifier.put(data.getShortName(), data);
        for (Bindable bindable : bindables.values()) {
            KeyBindingData data = byIdentifier.get(bindable.getIdentifier());
            if (data != null) this.bind(bindable, data.getKeyCode(), data.isControl(), data.isShift(),
                    data.isAlt(), data.isSuperKey());
        }
    }

    /** Assigns a chord and clears an existing binding in the same input scope. */
    public Bindable bind(Bindable target, int keyCode, boolean control, boolean shift,
                         boolean alt, boolean superKey) {
        Bindable conflict = null;
        if (keyCode != -1) {
            conflict = bindables.values().stream()
                    .filter(bindable -> bindable != target && bindable.getScope().equals(target.getScope()))
                    .filter(bindable -> bindable.hasChord(keyCode, control, shift, alt, superKey))
                    .findFirst().orElse(null);
            if (conflict != null) conflict.clear();
        }
        target.bind(keyCode, control, shift, alt, superKey);
        return conflict;
    }

    public Bindable reset(Bindable target) {
        target.reset();
        return this.bind(target, target.getKeyCode(), target.isControl(), target.isShift(),
                target.isAlt(), target.isSuperKey());
    }

    public List<Bindable> getBindables() {
        return List.copyOf(bindables.values());
    }

    public List<String> getCategories() {
        return new ArrayList<>(bindables.values().stream().map(Bindable::getCategory).distinct().toList());
    }
}
