package me.f1nal.trinity.gui.windows.impl.assembler.args;

import me.f1nal.trinity.execution.MemberDetails;

public class DetailsArgument extends InstructionOperand {
    private MemberDetails details;
    private final boolean method;

    public DetailsArgument(MemberDetails details, boolean method) {
        this.details = details;
        this.method = method;

        this.getDetailsText().addAll(details.asText(method));
    }

    public MemberDetails getDetails() {
        return details;
    }

    @Override
    public InstructionOperand copy() {
        return new DetailsArgument(this.details, this.method);
    }
}
