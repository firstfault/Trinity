package me.f1nal.trinity.gui.windows.impl.classstructure.nodes;

import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.classstructure.StructureKind;

public class ClassStructureNodeField extends AbstractClassStructureNodeInput<FieldInput> {
    public ClassStructureNodeField(FieldInput fieldInput) {
        super(FontAwesomeIcons.List, fieldInput);
    }

    @Override
    protected String getText() {
        return getInput().getDisplayName().getName();
    }

    @Override
    public StructureKind getKind() {
            return StructureKind.FIELD;
    }

    @Override
    protected void appendType(ColoredStringBuilder text, String suffix) {
        appendReturnType(text, getInput().getDescriptor(), suffix);
    }

    @Override
    protected void appendParameters(ColoredStringBuilder text) {

    }
}
