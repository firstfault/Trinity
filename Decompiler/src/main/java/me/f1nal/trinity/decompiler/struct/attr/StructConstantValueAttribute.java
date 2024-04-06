// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.struct.attr;

import me.f1nal.trinity.decompiler.struct.consts.ConstantPool;
import me.f1nal.trinity.decompiler.util.DataInputFullStream;

import java.io.IOException;

public class StructConstantValueAttribute extends StructGeneralAttribute {

  private int index;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool) throws IOException {
    index = data.readUnsignedShort();
  }

  public int getIndex() {
    return index;
  }
}
