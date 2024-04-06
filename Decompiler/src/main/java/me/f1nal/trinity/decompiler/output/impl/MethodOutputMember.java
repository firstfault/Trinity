package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.decompiler.output.OutputMember;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MethodOutputMember extends OutputMember implements IMemberDetails {
    private String ownerName;
    private String methodName, methodDescriptor;

    public MethodOutputMember(int length) {
        super(length);
    }

    public MethodOutputMember(int length, String ownerName, String methodName, String methodDescriptor) {
        super(length);
        this.ownerName = ownerName;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(ownerName);
        dataOutput.writeUTF(methodName);
        dataOutput.writeUTF(methodDescriptor);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        ownerName = dataInput.readUTF();
        methodName = dataInput.readUTF();
        methodDescriptor = dataInput.readUTF();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    @Override
    public String getName() {
        return this.getMethodName();
    }

    @Override
    public String getOwner() {
        return this.getOwnerName();
    }

    @Override
    public String getDesc() {
        return this.getMethodDescriptor();
    }
}
