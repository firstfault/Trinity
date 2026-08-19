package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.extern.IDecompilationProgressListener;
import me.f1nal.trinity.decompiler.output.impl.FieldOutputMember;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelMethodDecompilationTest {
    @Test
    void parallelMethodProcessingKeepsFinalSourceDeterministic() {
        byte[] classBytes = createClass(200);
        DecompileResult sequential = decompile(classBytes, 1);
        DecompileResult parallel = decompile(classBytes, 4);

        assertNotNull(sequential.source());
        assertEquals(sequential.source(), parallel.source());
        assertTrue(parallel.workerThreads().stream()
                .anyMatch(name -> name.startsWith("Fernflower Method ")));
        assertTrue(parallel.workerThreads().size() > 1,
                "Expected method analysis to use more than one worker");
        assertTrue(parallel.progressiveMethods() > 0,
                "Expected progressive source callbacks while workers were active");
        assertEquals(201, parallel.processedMethods(),
                "Every method must be processed regardless of viewport priority");
    }

    @Test
    void viewportMethodsPreemptClassfileOrderThenResumeIt() {
        byte[] classBytes = createClass(40);
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.METHOD_PROCESSING_THREADS, "1");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");

        List<String> startedMethods = new ArrayList<>();
        IDecompilationProgressListener listener = new IDecompilationProgressListener() {
            @Override
            public void methodDecompiled(String owner, String name, String descriptor,
                                         String content) {
            }

            @Override
            public List<MethodKey> priorityMethods(String owner) {
                return List.of(new MethodKey("method39", "(I)I"),
                        new MethodKey("method38", "(I)I"));
            }
        };
        ClassDecompileTask task = new ClassDecompileTask(
                classBytes, options, ignored -> { }, listener) {
            @Override
            public void startMethod(String methodName) {
                startedMethods.add(methodName);
            }
        };

        task.run();

        assertEquals(List.of("method39 (I)I", "method38 (I)I", "<init> ()V"),
                startedMethods.subList(0, 3));
    }

    @Test
    void staticFinalWritesInClassInitializerCarryFieldMetadata() {
        String owner = "fixture/StaticFieldWrites";
        byte[] classBytes = createStaticFieldWriteClass(owner);

        String source = decompile(classBytes, 2).source();
        String fieldTag = OutputMemberSerializer.serializeTags(
                new FieldOutputMember("VALUE".length(), owner, "VALUE", "I"));

        assertNotNull(source);
        // One tagged declaration plus both PUTSTATIC assignments in <clinit>.
        assertEquals(3, countOccurrences(source, fieldTag + "VALUE"));
    }

    private static DecompileResult decompile(byte[] classBytes, int threads) {
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.METHOD_PROCESSING_THREADS,
                Integer.toString(threads));
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
        options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "0");

        AtomicReference<String> source = new AtomicReference<>();
        Set<String> workerThreads = ConcurrentHashMap.newKeySet();
        AtomicInteger progressiveMethods = new AtomicInteger();
        AtomicInteger processedMethods = new AtomicInteger();
        IDecompilationProgressListener listener = new IDecompilationProgressListener() {
            @Override
            public void methodProcessed(String owner, String name, String descriptor) {
                processedMethods.incrementAndGet();
            }

            @Override
            public void methodDecompiled(String owner, String name, String descriptor,
                                         String content) {
                progressiveMethods.incrementAndGet();
            }
        };
        ClassDecompileTask task = new ClassDecompileTask(classBytes, options,
                content -> {
                    if (content != null) source.set(content);
                }, listener) {
            @Override
            public void startMethod(String methodName) {
                workerThreads.add(Thread.currentThread().getName());
            }
        };
        task.run();
        return new DecompileResult(source.get(), workerThreads, progressiveMethods.get(),
                processedMethods.get());
    }

    private static byte[] createClass(int methodCount) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "fixture/ParallelMethods",
                null, "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object",
                "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        for (int index = 0; index < methodCount; index++) {
            MethodVisitor method = writer.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "method" + index, "(I)I", null, null);
            method.visitCode();
            method.visitVarInsn(Opcodes.ILOAD, 0);
            method.visitLdcInsn(index);
            method.visitInsn(Opcodes.IADD);
            method.visitInsn(Opcodes.IRETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] createStaticFieldWriteClass(String owner) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner,
                null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "VALUE", "I", null, null).visitEnd();

        MethodVisitor initializer = writer.visitMethod(Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        initializer.visitCode();
        initializer.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "nanoTime", "()J", false);
        initializer.visitInsn(Opcodes.LCONST_0);
        initializer.visitInsn(Opcodes.LCMP);
        Label nonPositive = new Label();
        Label end = new Label();
        initializer.visitJumpInsn(Opcodes.IFLE, nonPositive);
        initializer.visitInsn(Opcodes.ICONST_1);
        initializer.visitFieldInsn(Opcodes.PUTSTATIC, owner, "VALUE", "I");
        initializer.visitJumpInsn(Opcodes.GOTO, end);
        initializer.visitLabel(nonPositive);
        initializer.visitInsn(Opcodes.ICONST_2);
        initializer.visitFieldInsn(Opcodes.PUTSTATIC, owner, "VALUE", "I");
        initializer.visitLabel(end);
        initializer.visitInsn(Opcodes.RETURN);
        initializer.visitMaxs(0, 0);
        initializer.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }

    private record DecompileResult(String source, Set<String> workerThreads,
                                   int progressiveMethods, int processedMethods) {
    }
}
