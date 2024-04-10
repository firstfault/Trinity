package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class KindOutputMember extends OutputMember {
    private KindType type;

    public KindOutputMember(int length) {
        super(length);
    }

    public KindOutputMember(int length, KindType type) {
        super(length);
        this.type = type;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(type.ordinal());
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        this.type = KindType.values()[dataInput.readUnsignedByte()];
    }

    public KindType getType() {
        return type;
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitKind(this);
    }

    public enum KindType {
        CLASS_INTERFACE,
        CLASS_ANNOTATION,
        CLASS_CLASSES,
        CLASS_ABSTRACT,
        CLASS_ENUM,
        ;
    }
}
