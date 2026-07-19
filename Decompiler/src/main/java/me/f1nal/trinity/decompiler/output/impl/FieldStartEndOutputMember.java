package me.f1nal.trinity.decompiler.output.impl;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.decompiler.output.OutputMember;
import me.f1nal.trinity.decompiler.output.OutputMemberVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FieldStartEndOutputMember extends OutputMember implements IMemberDetails {
  private boolean start;
  private String owner, desc, name;

  public FieldStartEndOutputMember(int length, String owner, String desc, String name) {
    super(length);
    this.start = true;
    this.owner = owner;
    this.desc = desc;
    this.name = name;
  }

  public FieldStartEndOutputMember(int length) {
    this(length, null, null, null);
    this.start = false;
  }

  public boolean isStart() {
    return start;
  }

  @Override
  protected void serializeImpl(DataOutput dataOutput) throws IOException {
    dataOutput.writeBoolean(start);
    if (start) {
      dataOutput.writeUTF(owner);
      dataOutput.writeUTF(name);
      dataOutput.writeUTF(desc);
    }
  }

  @Override
  protected void deserializeImpl(DataInput dataInput) throws IOException {
    start = dataInput.readBoolean();
    if (start) {
      owner = dataInput.readUTF();
      name = dataInput.readUTF();
      desc = dataInput.readUTF();
    }
  }

  @Override
  public void visit(OutputMemberVisitor visitor) {
    visitor.visitFieldStartEnd(this);
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
