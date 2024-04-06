package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ClassOutputMember extends OutputMember {
    private String className;
    private boolean isImport;

    public ClassOutputMember(int length) {
        super(length);
    }

    public ClassOutputMember(int length, String className, boolean isImport) {
        super(length);
        this.className = className;
        this.isImport = isImport;
    }

    public ClassOutputMember(int length, String className) {
        this(length, className, false);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.className);
        dataOutput.writeBoolean(isImport);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        this.className = dataInput.readUTF();
        isImport = dataInput.readBoolean();
    }

    public String getClassName() {
        return className;
    }

    public boolean isImport() {
        return isImport;
    }
}
