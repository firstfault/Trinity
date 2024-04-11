package me.f1nal.trinity.gui.windows.impl.classstructure.nodes;

import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.impl.classstructure.StructureKind;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.Type;

public class ClassStructureNodeMethod extends AbstractClassStructureNodeInput<MethodInput> {
    public ClassStructureNodeMethod(MethodInput methodInput) {
        super(FontAwesomeIcons.Code, methodInput);
    }

    @Override
    protected String getText() {
        return getInput().getDisplayName().getName();
    }

    @Override
    public StructureKind getKind() {
        return StructureKind.METHOD;
    }

    @Override
    protected void appendType(ColoredStringBuilder text, String suffix) {
        String descriptor = getInput().getDescriptor();
        int endIndex = descriptor.lastIndexOf(')');
        appendReturnType(text, descriptor.substring(endIndex + 1), suffix);
    }

    @Override
    protected void appendParameters(ColoredStringBuilder text) {
        Type[] argumentTypes = Type.getArgumentTypes(this.getInput().getDescriptor());

        text.text(CodeColorScheme.DISABLED, "(");
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argument = argumentTypes[i];
            appendReturnType(text, argument.getDescriptor(), (i != argumentTypes.length - 1) ? ", " : "");
        }
        text.text(CodeColorScheme.DISABLED, ")");
    }
}
