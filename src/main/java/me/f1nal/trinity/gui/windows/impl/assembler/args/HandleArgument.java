package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

public class HandleArgument extends InstructionOperand {
    private final Handle handle;

    public HandleArgument(Handle handle) {
        this.handle = handle;
        getDetailsText().add(new ColoredString("h_" + this.getTag(handle.getTag()).toLowerCase(), CodeColorScheme.KEYWORD));
        getDetailsText().addAll(new MemberDetails(handle.getOwner(), handle.getName(), handle.getDesc()).asText(handle.getTag() >= Opcodes.H_INVOKEVIRTUAL));
    }

    private final static String[] TAGS = {"GETFIELD", "GETSTATIC", "PUTFIELD", "PUTSTATIC", "INVOKEVIRTUAL", "INVOKESTATIC", "INVOKESPECIAL", "NEWINVOKESPECIAL", "INVOKEINTERFACE",};

    private String getTag(int tag) {
        return TAGS[tag - Opcodes.H_GETFIELD];
    }

    @Override
    public InstructionOperand copy() {
        return new HandleArgument(this.handle);
    }
}
