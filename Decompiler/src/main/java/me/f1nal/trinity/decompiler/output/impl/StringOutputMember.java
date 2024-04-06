package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class StringOutputMember extends OutputMember {
    public StringOutputMember(int length) {
        super(length);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {

    }
}
