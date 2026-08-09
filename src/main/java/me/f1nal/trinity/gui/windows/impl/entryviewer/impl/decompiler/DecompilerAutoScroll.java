package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class DecompilerAutoScroll {
    private final Input<?> input;
    private final AbstractInsnNode instruction;
    private final Integer variableIndex;
    private final Integer variableOccurrence;
    private final boolean variableDeclaration;
    private DecompilerComponent component;
    private boolean found;
    private boolean fallbackToClass;
    private boolean navigationPending = true;

    public DecompilerAutoScroll(Input<?> input, AbstractInsnNode instruction) {
        this(input, instruction, null, null, false);
    }

    private DecompilerAutoScroll(Input<?> input, AbstractInsnNode instruction,
                                 Integer variableIndex, Integer variableOccurrence,
                                 boolean variableDeclaration) {
        this.input = input;
        this.instruction = instruction;
        this.variableIndex = variableIndex;
        this.variableOccurrence = variableOccurrence;
        this.variableDeclaration = variableDeclaration;
    }

    public static DecompilerAutoScroll forVariable(MethodInput methodInput, int variableIndex,
                                                   int componentOccurrence) {
        return new DecompilerAutoScroll(methodInput, null, variableIndex,
                componentOccurrence, false);
    }

    public static DecompilerAutoScroll forVariableDeclaration(MethodInput methodInput,
                                                              int variableIndex) {
        return new DecompilerAutoScroll(methodInput, null, variableIndex, null, true);
    }

    public DecompilerComponent findComponent(DecompiledClass decompiledClass) {
        if (!this.found) {
            if (this.variableIndex != null && this.input instanceof MethodInput methodInput) {
                if (decompiledClass.isProgressive()) return null;
                this.component = this.variableDeclaration
                        ? decompiledClass.findVariableDeclarationComponent(methodInput, this.variableIndex)
                        : decompiledClass.findVariableComponent(
                                methodInput, this.variableIndex, this.variableOccurrence);
                if (this.component != null) {
                    this.found = true;
                    return this.component;
                }
            } else if (this.instruction != null && this.input instanceof MethodInput methodInput) {
                if (decompiledClass.isProgressive()) {
                    return null;
                }
                this.component = decompiledClass.findInstructionComponent(methodInput, this.instruction);
                if (this.component != null) {
                    this.found = true;
                    return this.component;
                }
            }
            this.component = this.findTargetComponent(decompiledClass, this.input);
            if (this.component != null) {
                this.found = true;
            } else if (!decompiledClass.isProgressive()) {
                this.found = true;
                if (this.input instanceof MemberInput<?>) {
                    this.fallbackToClass = true;
                    this.component = this.findTargetComponent(
                            decompiledClass, this.input.getOwningClass());
                }
            }
        }
        return getComponent();
    }

    public DecompilerComponent getComponent() {
        return component;
    }

    public boolean isNavigationPending() {
        return navigationPending;
    }

    public void markNavigated() {
        this.navigationPending = false;
    }

    public void invalidate() {
        this.component = null;
        this.found = false;
        this.fallbackToClass = false;
        this.navigationPending = true;
    }

    public boolean isFallbackToClass() {
        return fallbackToClass;
    }

    private DecompilerComponent findTargetComponent(DecompiledClass decompiledClass, Input<?> target) {
        List<DecompilerComponent> componentList = decompiledClass.getComponentList();
        for (DecompilerComponent component : componentList) {
            if (component.input == target) {
                return this.findDirectComponent(componentList, component, target);
            }
        }
        return null;
    }

    private DecompilerComponent findDirectComponent(List<DecompilerComponent> componentList,
                                                    DecompilerComponent marker, Input<?> target) {
        final int indexOf = componentList.indexOf(marker);
        for (int i = indexOf + 1; i < Math.min(indexOf + 20, componentList.size()); i++) {
            DecompilerComponent component = componentList.get(i);

            if (component.memberKey != null && component.memberKey.equals(target.toString())) {
                return component;
            }
        }
        return marker;
    }

    public Input<?> getInput() {
        return input;
    }
}
