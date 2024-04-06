// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.exps;

import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.ClassesProcessor;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.rels.ClassWrapper;
import me.f1nal.trinity.decompiler.main.rels.MethodWrapper;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExprUtil {
  public static List<VarVersionPair> getSyntheticParametersMask(String className, String descriptor, int parameters) {
    ClassesProcessor.ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(className);
    return node != null ? getSyntheticParametersMask(node, descriptor, parameters) : null;
  }

  public static List<VarVersionPair> getSyntheticParametersMask(ClassesProcessor.ClassNode node, String descriptor, int parameters) {
    List<VarVersionPair> mask = null;

    ClassWrapper wrapper = node.getWrapper();
    if (wrapper != null) {
      // own class
      MethodWrapper methodWrapper = wrapper.getMethodWrapper(CodeConstants.INIT_NAME, descriptor);
      if (methodWrapper == null) {
        if (DecompilerContext.getOption(IFernflowerPreferences.IGNORE_INVALID_BYTECODE)) {
          return null;
        }
        throw new RuntimeException("Constructor " + node.classStruct.qualifiedName + "." + CodeConstants.INIT_NAME + descriptor + " not found");
      }
      mask = methodWrapper.synthParameters;
    }
    else if (parameters > 0 && node.type == ClassesProcessor.ClassNode.CLASS_MEMBER && (node.access & CodeConstants.ACC_STATIC) == 0) {
      // non-static member class
      mask = new ArrayList<>(Collections.nCopies(parameters, null));
      mask.set(0, new VarVersionPair(-1, 0));
    }

    return mask;
  }
}