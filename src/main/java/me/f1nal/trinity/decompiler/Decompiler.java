package me.f1nal.trinity.decompiler;

import com.google.common.eventbus.Subscribe;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.main.Fernflower;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.extern.IDecompilationProgressListener;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class Decompiler implements IEventListener {
    private final Trinity trinity;
    private final Map<ClassInput, DecompiledClass> decompileCache = new ConcurrentHashMap<>();
    private final Set<ClassInput> decompileStack = ConcurrentHashMap.newKeySet();
    private final Set<ClassInput> failedList = ConcurrentHashMap.newKeySet();
    private final Map<ClassInput, Long> decompileGenerations = new ConcurrentHashMap<>();
    private final Map<Object, Long> memberRefreshGenerations = new ConcurrentHashMap<>();
    private final AtomicLong generationCounter = new AtomicLong();

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
            decompileStack.remove(classInput);
            DecompiledClass decompiledClass = decompileCache.get(classInput);
            if (decompiledClass != null) {
                decompiledClass.failProgressive();
            }
            throw ioException;
        }
    }

    private void decompileInternal(ClassInput classInput, Callback<DecompiledClass> decompileCallback) throws IOException {
        decompileStack.add(classInput);
        failedList.remove(classInput);
        long generation = generationCounter.incrementAndGet();
        decompileGenerations.put(classInput, generation);

        DecompiledClass progressiveClass = DecompiledClass.progressive(trinity, classInput);
        decompileCache.put(classInput, progressiveClass);

        Map<String, Object> options = createOptions();

        AtomicBoolean finished = new AtomicBoolean(false);
        Consumer<String> output = content -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }

            if (!Objects.equals(decompileGenerations.get(classInput), generation)) {
                return;
            }

            try {
                Objects.requireNonNull(content);
                Objects.requireNonNull(trinity);
                DecompiledClass decompiledClass = new DecompiledClass(trinity, classInput, content);
                progressiveClass.finishProgressive(decompiledClass);
                if (decompileCallback != null) {
                    decompileCallback.call(decompiledClass);
                }
                failedList.remove(classInput);
            } catch (Throwable e) {
                failedList.add(classInput);
                progressiveClass.failProgressive();
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

        IDecompilationProgressListener progressListener = (owner, name, descriptor, content) -> {
            if (!finished.get() && Objects.equals(decompileGenerations.get(classInput), generation)) {
                progressiveClass.queueMethodOutput(new MemberDetails(owner, name, descriptor), content);
            }
        };

        ClassDecompileTask classDecompileTask = new ClassDecompileTask(this.serializeClassBytes(classInput), options, output, progressListener);
        Thread thread = new Thread(classDecompileTask, "Decompiler");
        thread.start();
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        ClassInput classInput = event.getClassInput();
        DecompiledClass displayedClass = decompileCache.get(classInput);
        if (displayedClass == null) {
            return;
        }
        if (displayedClass.isProgressive() || isDecompiling(classInput)) {
            try {
                decompile(classInput, null);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            return;
        }

        MemberInput<?> memberInput = event.getMemberInput();
        Object generationKey = memberInput.getNode();
        long generation = generationCounter.incrementAndGet();
        memberRefreshGenerations.put(generationKey, generation);

        final byte[] classBytes;
        try {
            classBytes = serializeClassBytes(classInput);
        } catch (IOException exception) {
            memberRefreshGenerations.remove(generationKey, generation);
            exception.printStackTrace();
            return;
        }

        AtomicBoolean finished = new AtomicBoolean();
        Consumer<String> output = content -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            try {
                if (content == null || !Objects.equals(memberRefreshGenerations.get(generationKey), generation)
                        || decompileCache.get(classInput) != displayedClass) {
                    return;
                }
                DecompiledClass refreshedClass = new DecompiledClass(trinity, classInput, content);
                displayedClass.queueMemberReplacement(event.getPreviousDetails(), memberInput.getDetails(),
                        memberInput.getNode(), memberInput instanceof MethodInput, refreshedClass);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                memberRefreshGenerations.remove(generationKey, generation);
            }
        };

        ClassDecompileTask task = new ClassDecompileTask(
                classBytes, createOptions(), output, IDecompilationProgressListener.NONE);
        new Thread(task, "Decompiler Member Refresh").start();
    }

    private Map<String, Object> createOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        options.put(IFernflowerPreferences.DECOMPILE_ENUM, Main.getPreferences().isDecompilerEnumClass() ? "0" : "1");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");
        return options;
    }

    private byte[] serializeClassBytes(ClassInput classInput) throws IOException {
        try {
            ClassWriter classWriter = new ClassWriter(0);
            ClassNode classNodeCopy = new ClassNode();
            classInput.getNode().accept(classNodeCopy);
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
        decompileGenerations.remove(owningClass);
        decompileStack.remove(owningClass);
    }
}
