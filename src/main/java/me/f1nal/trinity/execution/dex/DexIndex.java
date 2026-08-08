package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.execution.Execution;

import java.io.IOException;
import java.util.HashSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Native DEX files and class identities loaded into one Trinity project. */
public final class DexIndex {
    private final Execution execution;
    private final Map<String, DexFileUnit> files = new LinkedHashMap<>();
    private final Map<String, DexClassEntry> classes = new LinkedHashMap<>();
    private final DexJavaDecompiler.Workspace javaDecompiler =
            new DexJavaDecompiler.Workspace();

    public DexIndex(Execution execution) {
        this.execution = Objects.requireNonNull(execution, "execution");
    }

    public DexFileUnit parse(String name, byte[] bytes) throws IOException {
        try {
            DexBackedDexFile dexFile = new DexBackedDexFile(null, bytes);
            DexFileUnit unit = new DexFileUnit(name, bytes, dexFile);
            for (ClassDef classDef : dexFile.getClasses()) {
                unit.addClass(new DexClassEntry(unit, classDef));
            }
            return unit;
        } catch (RuntimeException exception) {
            throw new IOException(String.format("Invalid DEX file %s: %s", name,
                    exception.getMessage()), exception);
        }
    }

    public void install(DexFileUnit unit) {
        Main.assertRenderThread();
        if (files.containsKey(unit.getName())) {
            throw new IllegalArgumentException(String.format("DEX file already exists: %s", unit.getName()));
        }
        Set<String> incomingClasses = new HashSet<>();
        for (DexClassEntry entry : unit.getClasses()) {
            DexClassEntry previous = classes.get(entry.getInternalName());
            if (!incomingClasses.add(entry.getInternalName()) || previous != null) {
                String previousFile = previous == null ? unit.getName() : previous.getFile().getName();
                throw new IllegalArgumentException(String.format(
                        "DEX class %s is defined by both %s and %s", entry.getInternalName(),
                        previousFile, unit.getName()));
            }
        }
        files.put(unit.getName(), unit);
        for (DexClassEntry entry : unit.getClasses()) {
            classes.put(entry.getInternalName(), entry);
            entry.setPackage(execution.getRootPackage(), false);
        }
        javaDecompiler.invalidate();
    }
    public void replace(DexFileUnit previous, DexFileUnit replacement) {
        Main.assertRenderThread();
        if (files.get(previous.getName()) != previous) {
            throw new IllegalArgumentException(String.format(
                    "DEX file is no longer current: %s", previous.getName()));
        }
        if (!previous.getName().equals(replacement.getName())) {
            throw new IllegalArgumentException("Replacement DEX file name does not match");
        }

        Set<String> previousClasses = new HashSet<>();
        previous.getClasses().forEach(entry -> previousClasses.add(entry.getInternalName()));
        Set<String> replacementClasses = new HashSet<>();
        for (DexClassEntry entry : replacement.getClasses()) {
            DexClassEntry collision = classes.get(entry.getInternalName());
            if (!replacementClasses.add(entry.getInternalName())
                    || (collision != null && collision.getFile() != previous)) {
                String collisionFile = collision == null
                        ? replacement.getName() : collision.getFile().getName();
                throw new IllegalArgumentException(String.format(
                        "DEX class %s collides with %s", entry.getInternalName(), collisionFile));
            }
        }
        if (!previousClasses.equals(replacementClasses)) {
            throw new IllegalArgumentException(
                    "Replacement DEX must preserve the complete class identity set");
        }

        for (DexClassEntry entry : previous.getClasses()) {
            entry.getPackage().remove(entry);
            classes.remove(entry.getInternalName(), entry);
        }
        files.put(replacement.getName(), replacement);
        for (DexClassEntry entry : replacement.getClasses()) {
            classes.put(entry.getInternalName(), entry);
            entry.setPackage(execution.getRootPackage(), false);
        }
        javaDecompiler.invalidate();
        execution.getTrinity().getEventManager().postEvent(new EventClassesLoaded());
    }


    public DexClassEntry getClass(String internalName) {
        return classes.get(normalizeClassName(internalName));
    }

    public Collection<DexClassEntry> getClasses() {
        return List.copyOf(classes.values());
    }

    public Collection<DexFileUnit> getFiles() {
        return List.copyOf(files.values());
    }

    public DexJavaDecompiler.Workspace getJavaDecompiler() {
        return javaDecompiler;
    }

    public void close() {
        javaDecompiler.close();
    }

    public int classCount() {
        return classes.size();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    private static String normalizeClassName(String value) {
        return value != null && value.startsWith("L") && value.endsWith(";")
                ? DexDescriptors.internalName(value) : value;
    }
}
