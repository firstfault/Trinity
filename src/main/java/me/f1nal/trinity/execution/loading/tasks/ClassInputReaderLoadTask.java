package me.f1nal.trinity.execution.loading.tasks;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.loading.ProgressiveLoadTask;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.gui.viewport.notifications.ICaption;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassInputReaderLoadTask extends ProgressiveLoadTask implements ICaption {
    private final List<byte[]> classBytes;
    private final Map<String, byte[]> resourceMap;

    public ClassInputReaderLoadTask(List<byte[]> classBytes, Map<String, byte[]> resourceMap) {
        super("Reading Input");
        this.classBytes = classBytes;
        this.resourceMap = resourceMap;
    }

    @Override
    public void runImpl() {
        this.startWork(classBytes.size());
        List<Runnable> tasks = new ArrayList<>();

        AtomicInteger classLoadFails = new AtomicInteger();

        classBytes.forEach(bytes -> {
            ClassNode classNode = this.readClassNode(bytes);
            if (classNode == null) {
                classLoadFails.incrementAndGet();
                return;
            }
            ClassTarget classTarget = this.createClassTarget(classNode, bytes.length, tasks);
            tasks.add(() -> getTrinity().getExecution().addClassTarget(classTarget));
            this.finishedWork();
        });

        if (classLoadFails.get() != 0) {
            Main.getDisplayManager().addNotification(new Notification(NotificationType.WARNING, this,
                    ColoredStringBuilder.create()
                            .fmt("Failed to load {} classes", classLoadFails.get()).get()));
        }

        tasks.add(() -> {
            resourceMap.forEach((name, bytes) -> getTrinity().getExecution().getResourceMap().put(name, bytes));
            getTrinity().getExecution().getResourceMap().forEach((name, bytes) -> new ResourceArchiveEntry(name, bytes).setPackage(getTrinity().getExecution().getRootPackage()));
        });

        Main.runLater(() -> {
            tasks.forEach(Runnable::run);
            synchronized (tasks) {
                tasks.notify();
            }
        });

        synchronized (tasks) {
            try {
                tasks.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ClassTarget createClassTarget(ClassNode classNode, int length, List<Runnable> tasks) {
        Execution execution = getTrinity().getExecution();
        ClassTarget classTarget = new ClassTarget(classNode.name, length);
        ClassInput classInput = new ClassInput(execution, classNode, classTarget);
        classTarget.setInput(classInput);
        tasks.add(() -> {
            classTarget.setPackage(execution.getRootPackage());
            execution.getClassList().add(classInput);
        });
        classInput.getNode().methods.forEach(method -> classInput.addInput(new MethodInput(method, classInput)));
        classInput.getNode().fields.forEach(field -> classInput.addInput(new FieldInput(field, classInput)));
        return classTarget;
    }

    private ClassNode readClassNode(byte[] bytes) {
        ClassNode classNode = this.readClassNode(bytes, 0);
        if (classNode == null) {
            return this.readClassNode(bytes, ClassReader.SKIP_DEBUG);
        }
        return classNode;
    }

    private ClassNode readClassNode(byte[] bytes, int flags) {
        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, flags);
            return classNode;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @Override
    public String getCaption() {
        return this.getName();
    }
}
