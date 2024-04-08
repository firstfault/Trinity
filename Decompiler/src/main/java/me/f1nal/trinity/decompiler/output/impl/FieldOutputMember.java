package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FieldOutputMember extends OutputMember implements IMemberDetails {
    private String ownerName;
    private String fieldName, fieldDescriptor;

    public FieldOutputMember(int length) {
        super(length);
    }

    public FieldOutputMember(int length, String ownerName, String fieldName, String fieldDescriptor) {
        super(length);
        this.ownerName = ownerName;
        this.fieldName = fieldName;
        this.fieldDescriptor = fieldDescriptor;
    }

    @Override
    protected void serializeImpl(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(ownerName);
        dataOutput.writeUTF(fieldName);
        dataOutput.writeUTF(fieldDescriptor);
    }

    @Override
    protected void deserializeImpl(DataInput dataInput) throws IOException {
        ownerName = dataInput.readUTF();
        fieldName = dataInput.readUTF();
        fieldDescriptor = dataInput.readUTF();
    }

    @Override
    public void visit(OutputMemberVisitor visitor) {
        visitor.visitField(this);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldDescriptor() {
        return fieldDescriptor;
    }

    @Override
    public String getOwner() {
        return getOwnerName();
    }

    @Override
    public String getName() {
        return getFieldName();
    }

    @Override
    public String getDesc() {
        return getFieldDescriptor();
    }
}
