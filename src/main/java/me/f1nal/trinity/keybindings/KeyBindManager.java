package me.f1nal.trinity.keybindings;

import java.util.ArrayList;
import java.util.List;

public class KeyBindManager {
    private final List<Bindable> bindables = new ArrayList<>();

    public final Bindable ASSEMBLER_INSERT = bindable("Assembler Insert", "assembler.insert");

    private Bindable bindable(String name, String identifier) {
        Bindable bindable = new Bindable(identifier, name);
        bindables.add(bindable);
        return bindable;
    }

    public List<Bindable> getBindables() {
        return bindables;
    }
}
