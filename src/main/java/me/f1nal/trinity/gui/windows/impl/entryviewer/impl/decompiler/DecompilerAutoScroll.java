package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class DecompilerAutoScroll {
    private final Input<?> input;
    private final AbstractInsnNode instruction;
    private DecompilerComponent component;
    private boolean found;

    public DecompilerAutoScroll(Input<?> input, AbstractInsnNode instruction) {
        this.input = input;
        this.instruction = instruction;
    }

    public DecompilerComponent findComponent(DecompiledClass decompiledClass) {
        if (!this.found) {
            if (this.instruction != null && this.input instanceof MethodInput methodInput) {
                if (decompiledClass.isProgressive()) {
                    return null;
                }
                this.component = decompiledClass.findInstructionComponent(methodInput, this.instruction);
                if (this.component != null) {
                    this.found = true;
                    return this.component;
                }
            }
            this.component = this.findTargetComponent(decompiledClass);
            this.found = true;
        }
        return getComponent();
    }

    public DecompilerComponent getComponent() {
        return component;
    }

    private DecompilerComponent findTargetComponent(DecompiledClass decompiledClass) {
        List<DecompilerComponent> componentList = decompiledClass.getComponentList();
        for (DecompilerComponent component : componentList) {
            if (component.input == this.input) {
                return this.findDirectComponent(componentList, component);
            }
        }
        return null;
    }

    private DecompilerComponent findDirectComponent(List<DecompilerComponent> componentList, DecompilerComponent marker) {
        final int indexOf = componentList.indexOf(marker);
        for (int i = indexOf + 1; i < Math.min(indexOf + 20, componentList.size()); i++) {
            DecompilerComponent component = componentList.get(i);

            if (component.memberKey != null && component.memberKey.equals(this.input.toString())) {
                return component;
            }
        }
        return marker;
    }

    public Input<?> getInput() {
        return input;
    }
}
