package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class VariableOutputMember extends OutputMember {
    private int var;
    private String type;
    
    public VariableOutputMember(int length) {
        super(length);
    }

    public VariableOutputMember(int length, int var, String type) {
        super(length);
        this.var = var;
        this.type = type;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(var);
        dataOutput.writeUTF(type);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        var = dataInput.readInt();
        type = dataInput.readUTF();
    }

    public int getVar() {
        return var;
    }

    public String getType() {
        return type;
    }
}
