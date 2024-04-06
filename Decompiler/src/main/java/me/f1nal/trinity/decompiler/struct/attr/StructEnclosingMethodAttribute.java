// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.struct.attr;

import me.f1nal.trinity.decompiler.struct.consts.ConstantPool;
import me.f1nal.trinity.decompiler.struct.consts.LinkConstant;
import me.f1nal.trinity.decompiler.util.DataInputFullStream;

import java.io.IOException;

public class StructEnclosingMethodAttribute extends StructGeneralAttribute {

  private String className;
  private String methodName;
  private String methodDescriptor;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool) throws IOException {
    int classIndex = data.readUnsignedShort();
    int methodIndex = data.readUnsignedShort();

    className = pool.getPrimitiveConstant(classIndex).getString();
    if (methodIndex != 0) {
      LinkConstant lk = pool.getLinkConstant(methodIndex);
      methodName = lk.elementname;
      methodDescriptor = lk.descriptor;
    }
  }

  public String getClassName() {
    return className;
  }

  public String getMethodDescriptor() {
    return methodDescriptor;
  }

  public String getMethodName() {
    return methodName;
  }
}
