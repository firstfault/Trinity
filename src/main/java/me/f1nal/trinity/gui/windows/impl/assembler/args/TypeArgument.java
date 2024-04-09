package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

public class TypeArgument extends AbstractInsnArgument {
    private final String className;
    
    public TypeArgument(String className) {
        this.className = className;
        getDetailsText().add(new ColoredString(className, CodeColorScheme.CLASS_REF));
    }

    @Override
    public AbstractInsnArgument copy() {
        return new TypeArgument(this.className);
    }
}
