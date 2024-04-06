package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.main.Fernflower;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.patch.ClassPatchManager;
import me.f1nal.trinity.util.java.Callback;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Decompiler implements IEventListener {
    private final Trinity trinity;
    private int decompileId;
    private final Map<ClassInput, DecompiledClass> decompileCache = new HashMap<>();
    private Set<ClassInput> decompileStack = new HashSet<>();
    private Set<ClassInput> failedList = new HashSet<>();

    public Decompiler(Trinity trinity) throws FileNotFoundException {
        this.trinity = trinity;
    }

    public boolean isDecompiling(ClassInput classInput) {
        return decompileStack.contains(classInput);
    }

    public boolean isDecompileFailed(ClassInput classInput) {
        return failedList.contains(classInput);
    }

    public void decompile(ClassInput classInput, Callback<DecompiledClass> decompileCallback) throws IOException {
        try {
            decompileInternal(classInput, decompileCallback);
        } catch (IOException ioException) {
            failedList.add(classInput);
            throw ioException;
        }
    }

    private void decompileInternal(ClassInput classInput, Callback<DecompiledClass> decompileCallback) throws IOException {
        decompileStack.add(classInput);

        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        options.put(IFernflowerPreferences.DECOMPILE_ENUM, Main.getPreferences().isDecompilerEnumClass() ? "0" : "1");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");

        AtomicBoolean finished = new AtomicBoolean(false);
        Consumer<String> output = content -> {
            if (finished.get()) {
                return;
            }
            finished.set(true);

            try {
                Objects.requireNonNull(content);
                DecompiledClass decompiledClass = new DecompiledClass(trinity, classInput, content);
                decompileCache.put(classInput, decompiledClass);
                if (decompileCallback != null) {
                    decompileCallback.call(decompiledClass);
                }
                failedList.remove(classInput);
            } catch (Throwable e) {
                failedList.add(classInput);
                e.printStackTrace();
                if (decompileCallback != null) {
                    try {
                        decompileCallback.call(null);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
            decompileStack.remove(classInput);
        };

        ClassDecompileTask classDecompileTask = new ClassDecompileTask(this.serializeClassBytes(classInput), options, output);
        Thread thread = new Thread(classDecompileTask, "Decompiler");
        thread.start();
    }

    private byte[] serializeClassBytes(ClassInput classInput) throws IOException {
        try {
            ClassWriter classWriter = new ClassWriter(0);
            ClassNode classNodeCopy = new ClassNode();
            classInput.getClassNode().accept(classNodeCopy);
            ClassPatchManager.getClassPatchList().stream().filter(cp -> cp.isEnabled(classNodeCopy)).forEach(cp -> cp.patch(classNodeCopy));
            classNodeCopy.accept(classWriter);
            return classWriter.toByteArray();
        } catch (Throwable throwable) {
            throw new IOException("Failed to write class file", throwable);
        }
    }

    public @Nullable DecompiledClass getFromCache(ClassInput classInput) {
        return decompileCache.get(classInput);
    }

    public void invalidateCache(ClassInput owningClass) {
        decompileCache.remove(owningClass);
    }
}
