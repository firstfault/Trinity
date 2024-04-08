package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class KeywordOutputMember extends OutputMember {
    public KeywordOutputMember(int length) {
        super(length);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {

    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {

    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitKeyword(this);
    }
}
