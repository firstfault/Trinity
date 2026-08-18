package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
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
        ClassDecompileTask task = new ClassDecompileTask(classBytes, options,
                content -> {
                    if (content != null) source.set(content);
                }, (owner, name, descriptor, content) ->
                progressiveMethods.incrementAndGet()) {
            @Override
            public void startMethod(String methodName) {
                workerThreads.add(Thread.currentThread().getName());
            }
        };
        task.run();
        return new DecompileResult(source.get(), workerThreads, progressiveMethods.get());
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

    private record DecompileResult(String source, Set<String> workerThreads,
                                   int progressiveMethods) {
    }
}
