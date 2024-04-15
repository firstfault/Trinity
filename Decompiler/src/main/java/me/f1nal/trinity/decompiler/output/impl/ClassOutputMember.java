package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ClassOutputMember extends OutputMember {
    private String className;
    private int flags;

    public ClassOutputMember(int length) {
        super(length);
    }

    public ClassOutputMember(int length, String className, int flags) {
        super(length);
        this.className = className;
        this.flags = flags;
    }

    public ClassOutputMember(int length, String className) {
        this(length, className, 0);
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.className);
        dataOutput.writeByte(flags);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        this.className = dataInput.readUTF();
        this.flags = dataInput.readByte();
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitClass(this);
    }

    public String getClassName() {
        return className;
    }

    public boolean isImport() {
        return (flags & FLAG_IMPORT) != 0;
    }

    public boolean isKeepText() {
        return (flags & FLAG_KEEPTEXT) != 0;
    }

    public int getFlags() {
        return flags;
    }

    /**
     * This member is an import declaration.
     */
    public static final int FLAG_IMPORT = 1 << 1;
    /**
     * Keep original text regardless of class name.
     */
    public static final int FLAG_KEEPTEXT = 1 << 2;
}
