// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler;

import me.f1nal.trinity.decompiler.util.ListStack;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;

public class ExpressionStack extends ListStack<Exprent> {
  public ExpressionStack() { }

  private ExpressionStack(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public ExpressionStack copy() {
    ExpressionStack copy = new ExpressionStack(size());
    for (Exprent expr : this) copy.push(expr.copy());
    return copy;
  }
}
