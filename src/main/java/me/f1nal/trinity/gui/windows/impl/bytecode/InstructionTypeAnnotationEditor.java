package me.f1nal.trinity.gui.windows.impl.bytecode;

import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.List;

/** Reusable instruction-level facade over the bytecode editor's type-annotation UI. */
public final class InstructionTypeAnnotationEditor {
    private final BytecodeEditorSupport.TypeAnnotationListEditor delegate;

    public InstructionTypeAnnotationEditor(String label, List<TypeAnnotationNode> annotations) {
        this.delegate = new BytecodeEditorSupport.TypeAnnotationListEditor(label, annotations);
    }

    public void draw() {
        delegate.draw();
    }

    public List<TypeAnnotationNode> get() {
        return delegate.get();
    }
}
