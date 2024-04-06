package me.f1nal.trinity.gui.frames.impl.assembler.stack.processor;

import me.f1nal.trinity.gui.frames.impl.assembler.stack.JavaStack;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InstructionStackProcessor<T extends AbstractInsnNode> {
    private BiConsumer<T, JavaStack> stackProcessor;

    public void stack(Consumer<JavaStack> stack) {
        this.stackProcessor = (t, javaStack) -> stack.accept(javaStack);
    }

    public void stack(BiConsumer<T, JavaStack> stack) {
        this.stackProcessor = stack;
    }

    public final void process(AbstractInsnNode insnNode, JavaStack stack) {
        if (stackProcessor != null) {
            //noinspection unchecked
            stackProcessor.accept((T) insnNode, stack);
        }
    }
}
