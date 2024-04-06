package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.xref.where.XrefWhere;
import org.objectweb.asm.util.Printer;

public class ClassXref extends AbstractXref {
    private final XrefAccessType accessType;
    private final String invocation;

    protected ClassXref(XrefWhere where, XrefAccessType accessType, String invocation, XrefKind kind) {
        super(where, kind);
        this.accessType = accessType;
        this.invocation = invocation;
    }

    public static ClassXref extendsClass(XrefWhere where, boolean implementsItf) {
        return new ClassXref(where, XrefAccessType.READ, implementsItf ? "Implements" : "Extends", XrefKind.INHERIT);
    }

    public static ClassXref returnsClass(XrefWhere where) {
        return new ClassXref(where, XrefAccessType.READ, "Returns", XrefKind.RETURN);
    }

    public static ClassXref parameter(XrefWhere where) {
        return new ClassXref(where, XrefAccessType.READ, "Parameter", XrefKind.PARAMETER);
    }

    public static ClassXref classLiteral(XrefWhere where) {
        return new ClassXref(where, XrefAccessType.READ, ".class", XrefKind.LITERAL);
    }

    public static ClassXref typeInstruction(XrefWhere where, int opcode) {
        return new ClassXref(where, XrefAccessType.READ, Printer.OPCODES[opcode], XrefKind.TYPE);
    }

    @Override
    public XrefAccessType getAccess() {
        return this.accessType;
    }

    @Override
    public String getInvocation() {
        return this.invocation;
    }
}
