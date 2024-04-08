package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FieldDeclarationOutputMember extends OutputMember implements IMemberDetails {
    private String owner, desc, name;

    public FieldDeclarationOutputMember(int length, String owner, String desc, String name) {
        super(length);
        this.owner = owner;
        this.desc = desc;
        this.name = name;
    }

    public FieldDeclarationOutputMember(int length) {
        this(length, null, null, null);
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(getOwner());
        dataOutput.writeUTF(getName());
        dataOutput.writeUTF(getDesc());
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        this.owner = dataInput.readUTF();
        this.name = dataInput.readUTF();
        this.desc = dataInput.readUTF();
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitFieldDeclaration(this);
    }
}
