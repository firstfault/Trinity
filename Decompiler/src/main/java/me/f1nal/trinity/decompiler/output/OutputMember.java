package me.f1nal.trinity.decompiler.output;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class OutputMember {
    private int length;

    public OutputMember(int length) {
        this.length = length;
    }

    public final int getLength() {
        return length;
    }

    protected abstract void serializeImpl(DataOutput dataOutput) throws IOException;
    protected abstract void deserializeImpl(DataInput dataInput) throws IOException;

    public abstract void visit(OutputMemberVisitor visitor);

    public void serialize(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(this.length);
        serializeImpl(dataOutput);
    }

    public void deserialize(DataInput dataInput) throws IOException {
        deserializeImpl(dataInput);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
