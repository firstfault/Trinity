// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.main.rels;

import me.f1nal.trinity.decompiler.main.collectors.CounterContainer;
import me.f1nal.trinity.decompiler.modules.decompiler.sforms.DirectGraph;
import me.f1nal.trinity.decompiler.modules.decompiler.sforms.FlattenStatementsHelper;
import me.f1nal.trinity.decompiler.modules.decompiler.stats.RootStatement;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;
import me.f1nal.trinity.decompiler.struct.StructMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodWrapper {
  public final RootStatement root;
  public final VarProcessor varproc;
  public final StructMethod methodStruct;
  public final CounterContainer counter;
  public final Set<String> setOuterVarNames = new HashSet<>();

  public DirectGraph graph;
  public List<VarVersionPair> synthParameters;
  public boolean decompiledWithErrors;
  public String errorStacktrace;

  public MethodWrapper(RootStatement root, VarProcessor varproc, StructMethod methodStruct, CounterContainer counter) {
    this.root = root;
    this.varproc = varproc;
    this.methodStruct = methodStruct;
    this.counter = counter;
  }

  public void setErrorStacktrace(Throwable throwable) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    this.errorStacktrace = sw.toString();
  }

  public DirectGraph getOrBuildGraph() {
    if (graph == null && root != null) {
      graph = new FlattenStatementsHelper().buildDirectGraph(root);
    }
    return graph;
  }

  @Override
  public String toString() {
    return methodStruct.getName();
  }
}