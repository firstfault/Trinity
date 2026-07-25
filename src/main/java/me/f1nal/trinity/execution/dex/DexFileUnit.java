package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One original DEX payload and the native class definitions parsed from it. */
public final class DexFileUnit {
    private final String name;
    private final byte[] bytes;
    private final DexBackedDexFile dexFile;
    private final List<DexClassEntry> classes = new ArrayList<>();

    DexFileUnit(String name, byte[] bytes, DexBackedDexFile dexFile) {
        this.name = Objects.requireNonNull(name, "name");
        this.bytes = Objects.requireNonNull(bytes, "bytes");
        this.dexFile = Objects.requireNonNull(dexFile, "dexFile");
    }

    void addClass(DexClassEntry entry) {
        classes.add(entry);
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public DexBackedDexFile getDexFile() {
        return dexFile;
    }

    public List<DexClassEntry> getClasses() {
        return List.copyOf(classes);
    }
}
