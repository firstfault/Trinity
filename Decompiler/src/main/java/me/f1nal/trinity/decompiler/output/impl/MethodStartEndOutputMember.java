package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.decompiler.output.OutputMember;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MethodStartEndOutputMember extends OutputMember implements IMemberDetails {
    private boolean start;
    private String owner, desc, name;

    public MethodStartEndOutputMember(int length, String owner, String desc, String name) {
        super(length);
        this.start = true;
        this.owner = owner;
        this.desc = desc;
        this.name = name;
    }

    public MethodStartEndOutputMember(int length) {
        this(length, null, null, null);
        this.start = false;
    }

    public boolean isStart() {
        return start;
    }

    public boolean isEnd() {
        return !isStart();
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeBoolean(this.start);
        if (this.start) {
            dataOutput.writeUTF(getOwner());
            dataOutput.writeUTF(getName());
            dataOutput.writeUTF(getDesc());
        }
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        this.start = dataInput.readBoolean();
        if (start) {
            this.owner = dataInput.readUTF();
            this.name = dataInput.readUTF();
            this.desc = dataInput.readUTF();
        }
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public String getName() {
        return name;
    }
}
