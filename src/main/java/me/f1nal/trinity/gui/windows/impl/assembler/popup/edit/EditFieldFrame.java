package me.f1nal.trinity.gui.windows.impl.assembler.popup.edit;

import me.f1nal.trinity.execution.labels.LabelTable;
import me.f1nal.trinity.execution.labels.MethodLabel;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.function.Function;

final class EditFieldFrame extends EditFieldText<AssemblerFrameCodec.FrameData> {
    private final FrameNode frame;
    private final LabelTable labels;
    private final Function<MethodLabel, LabelNode> resolver;

    EditFieldFrame(FrameNode frame, LabelTable labels, Function<MethodLabel, LabelNode> resolver) {
        super(8192, "Frame", "type=F_FULL, locals=[INTEGER], stack=[]",
                () -> new AssemblerFrameCodec.FrameData(frame.type, frame.local, frame.stack), data -> {
                    frame.type = data.type();
                    frame.local = new ArrayList<>(data.locals());
                    frame.stack = new ArrayList<>(data.stack());
                });
        this.frame = frame;
        this.labels = labels;
        this.resolver = resolver;
    }

    @Override
    protected AssemblerFrameCodec.FrameData parse(String input) throws InvalidEditInputException {
        try {
            return AssemblerFrameCodec.parse(input, labels, resolver);
        } catch (IllegalArgumentException exception) {
            throw new InvalidEditInputException(exception.getMessage());
        }
    }

    @Override
    public void updateField() {
        text.set(AssemblerFrameCodec.format(frame, labels));
    }
}
