package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class VariableOutputMember extends OutputMember {
    private int var;
    private String type;
    private boolean definition;
    
    public VariableOutputMember(int length) {
        super(length);
    }

    public VariableOutputMember(int length, int var, String type) {
        this(length, var, type, false);
    }

    public VariableOutputMember(int length, int var, String type, boolean definition) {
        super(length);
        this.var = var;
        this.type = type;
        this.definition = definition;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(var);
        dataOutput.writeUTF(type);
        dataOutput.writeBoolean(definition);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        var = dataInput.readInt();
        type = dataInput.readUTF();
        definition = dataInput.readBoolean();
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitVariable(this);
    }

    public int getVar() {
        return var;
    }

    public String getType() {
        return type;
    }

    public boolean isDefinition() {
        return definition;
    }
}
