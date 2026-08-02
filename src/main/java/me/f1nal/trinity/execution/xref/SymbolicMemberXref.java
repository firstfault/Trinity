package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.xref.where.XrefWhere;

/** A member reference represented by metadata rather than a direct field/method instruction. */
final class SymbolicMemberXref extends AbstractXref {
    private final XrefAccessType access;
    private final String invocation;

    SymbolicMemberXref(XrefWhere where, XrefKind kind,
                       XrefAccessType access, String invocation) {
        super(where, kind);
        this.access = access;
        this.invocation = invocation;
    }

    @Override
    public XrefAccessType getAccess() {
        return access;
    }

    @Override
    public String getInvocation() {
        return invocation;
    }
}
