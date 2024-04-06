// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.stats;

import me.f1nal.trinity.decompiler.main.rels.ClassWrapper;
import me.f1nal.trinity.decompiler.main.rels.MethodWrapper;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.InvocationExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;

public final class Statements {
  public static Statement findFirstData(Statement stat) {
    if (stat.getExprents() != null) {
      return stat;
    }
    else if (stat.isLabeled()) { // FIXME: Why??
      return null;
    }

    return switch (stat.type) {
      case SEQUENCE, IF, ROOT, SWITCH, SYNCHRONIZED -> findFirstData(stat.getFirst());
      default -> null;
    };
  }

  public static boolean isInvocationInitConstructor(InvocationExprent inv, MethodWrapper method, ClassWrapper wrapper, boolean withThis) {
    if (inv.getFuncType() == InvocationExprent.TYPE_INIT && inv.getInstance().type == Exprent.EXPRENT_VAR) {
      VarExprent instVar = (VarExprent)inv.getInstance();
      VarVersionPair varPair = new VarVersionPair(instVar);
      String className = method.varproc.getThisVars().get(varPair);
      if (className != null) { // any this instance. TODO: Restrict to current class?
        return withThis || !wrapper.getClassStruct().qualifiedName.equals(inv.getClassName());
      }
    }

    return false;
  }
}