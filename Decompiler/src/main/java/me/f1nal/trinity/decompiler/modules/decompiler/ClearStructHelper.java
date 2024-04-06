// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler;

import me.f1nal.trinity.decompiler.modules.decompiler.stats.RootStatement;
import me.f1nal.trinity.decompiler.modules.decompiler.stats.Statement;

import java.util.LinkedList;


public final class ClearStructHelper {

  public static void clearStatements(RootStatement root) {

    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(root);

    while (!stack.isEmpty()) {

      Statement stat = stack.removeFirst();

      stat.clearTempInformation();

      stack.addAll(stat.getStats());
    }
  }
}
