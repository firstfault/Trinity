package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PackageOutputMember extends OutputMember {
    private String className;
    private boolean parent;

    public PackageOutputMember(int length, String className, boolean parent) {
        super(length);
        this.className = className;
        this.parent = parent;
    }

    public PackageOutputMember(int length) {
        super(length);
    }

    public String getClassName() {
        return className;
    }

    public boolean isParent() {
        return parent;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeBoolean(parent);
        if (!parent) {
            dataOutput.writeUTF(className);
        }
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        parent = dataInput.readBoolean();
        if (!parent) {
            className = dataInput.readUTF();
        }
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitPackage(this);
    }
}
