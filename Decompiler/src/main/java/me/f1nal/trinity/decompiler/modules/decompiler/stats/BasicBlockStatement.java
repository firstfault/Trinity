// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.stats;

import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.code.Instruction;
import me.f1nal.trinity.decompiler.code.SimpleInstructionSequence;
import me.f1nal.trinity.decompiler.code.cfg.BasicBlock;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.main.collectors.CounterContainer;
import me.f1nal.trinity.decompiler.modules.decompiler.ExprProcessor;
import me.f1nal.trinity.decompiler.util.TextBuffer;

public class BasicBlockStatement extends Statement {
  private final BasicBlock block;

  public BasicBlockStatement(BasicBlock block) {
    super(StatementType.BASIC_BLOCK, block.id);
    this.block = block;

    CounterContainer container = DecompilerContext.getCounterContainer();
    if (id >= container.getCounter(CounterContainer.STATEMENT_COUNTER)) {
      container.setCounter(CounterContainer.STATEMENT_COUNTER, id + 1);
    }

    Instruction instr = block.getLastInstruction();
    if (instr != null) {
      if (instr.group == CodeConstants.GROUP_JUMP && instr.opcode != CodeConstants.opc_goto) {
        lastBasicType = StatementType.IF;
      }
      else if (instr.group == CodeConstants.GROUP_SWITCH) {
        lastBasicType = StatementType.SWITCH;
      }
    }

    buildMonitorFlags();
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
    TextBuffer tb = ExprProcessor.listToJava(varDefinitions, indent, tracer);
    tb.append(ExprProcessor.listToJava(exprents, indent, tracer));
    return tb;
  }

  @Override
  public Statement getSimpleCopy() {
    int id = DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER);

    SimpleInstructionSequence seq = new SimpleInstructionSequence();
    for (int i = 0; i < block.getSeq().length(); i++) {
      seq.addInstruction(block.getSeq().getInstr(i).clone(), -1);
    }

    return new BasicBlockStatement(new BasicBlock(id, seq));
  }

  public BasicBlock getBlock() {
    return block;
  }
}
