package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class BytecodeMarkerOutputMember extends OutputMember {
    private int method;
    private int opcode;
    private int offsetFromStart;
    
    public BytecodeMarkerOutputMember(int length, int method, int opcode, int offsetFromStart) {
        super(length);
        this.method = method;
        this.opcode = opcode;
        this.offsetFromStart = offsetFromStart;
    }

    public BytecodeMarkerOutputMember(int length) {
        super(length);
    }

    public static int getHashcode(String owner, String desc, String name) {
        return Objects.hash(owner, desc, name);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(method);
        dataOutput.writeByte(opcode);
        dataOutput.writeInt(offsetFromStart);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        method = dataInput.readInt();
        opcode = dataInput.readUnsignedByte();
        offsetFromStart = dataInput.readInt();
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitBytecodeMarker(this);
    }

    public int getMethod() {
        return method;
    }

    public int getOpcode() {
        return opcode;
    }

    public int getOffsetFromStart() {
        return offsetFromStart;
    }
}
