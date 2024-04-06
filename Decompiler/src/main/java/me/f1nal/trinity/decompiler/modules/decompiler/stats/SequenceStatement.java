// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.stats;

import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.modules.decompiler.DecHelper;
import me.f1nal.trinity.decompiler.modules.decompiler.ExprProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.StatEdge;
import me.f1nal.trinity.decompiler.util.TextBuffer;

import java.util.Arrays;
import java.util.List;


public class SequenceStatement extends Statement {


  // *****************************************************************************
  // constructors
  // *****************************************************************************

  private SequenceStatement() {
    super(StatementType.SEQUENCE);
  }

  public SequenceStatement(List<? extends Statement> lst) {

    this();

    lastBasicType = lst.get(lst.size() - 1).getLastBasicType();

    for (Statement st : lst) {
      stats.addWithKey(st, st.id);
    }

    first = stats.get(0);
  }

  private SequenceStatement(Statement head, Statement tail) {

    this(Arrays.asList(head, tail));

    List<StatEdge> lstSuccs = tail.getSuccessorEdges(StatEdge.EdgeType.DIRECT_ALL);
    if (!lstSuccs.isEmpty()) {
      StatEdge edge = lstSuccs.get(0);

      if (edge.getType() == StatEdge.EdgeType.REGULAR && edge.getDestination() != head) {
        post = edge.getDestination();
      }
    }
  }


  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public static Statement isHead2Block(Statement head) {

    if (head.getLastBasicType() != StatementType.GENERAL) {
      return null;
    }

    // at most one outgoing edge
    StatEdge edge = null;
    List<StatEdge> lstSuccs = head.getSuccessorEdges(StatEdge.EdgeType.DIRECT_ALL);
    if (!lstSuccs.isEmpty()) {
      edge = lstSuccs.get(0);
    }

    if (edge != null && edge.getType() == StatEdge.EdgeType.REGULAR) {
      Statement stat = edge.getDestination();

      if (stat != head && stat.getPredecessorEdges(StatEdge.EdgeType.REGULAR).size() == 1
          && !stat.isMonitorEnter()) {

        if (stat.getLastBasicType() == StatementType.GENERAL) {
          if (DecHelper.checkStatementExceptions(Arrays.asList(head, stat))) {
            return new SequenceStatement(head, stat);
          }
        }
      }
    }

    return null;
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
    TextBuffer buf = new TextBuffer();
    boolean isLabeled = isLabeled();

    buf.append(ExprProcessor.listToJava(varDefinitions, indent, tracer));

    if (isLabeled) {
      buf.appendIndent(indent++).append("label").append(Integer.toString(id)).append(": {").appendLineSeparator();
      tracer.incrementCurrentSourceLine();
    }

    boolean notEmpty = false;

    for (int i = 0; i < stats.size(); i++) {

      Statement stat = stats.get(i);

      if (i > 0 && notEmpty) {
        buf.appendLineSeparator();
        tracer.incrementCurrentSourceLine();
      }

      TextBuffer str = ExprProcessor.jmpWrapper(stat, indent, false, tracer);
      buf.append(str);

      notEmpty = !str.containsOnlyWhitespaces();
    }

    if (isLabeled) {
      buf.appendIndent(indent - 1).append("}").appendLineSeparator();
      tracer.incrementCurrentSourceLine();
    }

    return buf;
  }

  @Override
  public Statement getSimpleCopy() {
    return new SequenceStatement();
  }
}
