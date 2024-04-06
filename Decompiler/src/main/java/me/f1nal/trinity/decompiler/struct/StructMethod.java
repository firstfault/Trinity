// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.struct;

import me.f1nal.trinity.decompiler.code.*;
import me.f1nal.trinity.decompiler.struct.attr.StructCodeAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructGeneralAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructLocalVariableTableAttribute;
import me.f1nal.trinity.decompiler.struct.consts.ConstantPool;
import me.f1nal.trinity.decompiler.struct.gen.MethodDescriptor;
import me.f1nal.trinity.decompiler.struct.gen.Type;
import me.f1nal.trinity.decompiler.util.DataInputFullStream;
import me.f1nal.trinity.decompiler.util.VBStyleCollection;
import me.f1nal.trinity.decompiler.code.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
  method_info {
    u2 access_flags;
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
  }
*/
public class StructMethod extends StructMember {
  public static StructMethod create(DataInputFullStream in, ConstantPool pool, String clQualifiedName, int bytecodeVersion, boolean own) throws IOException {
    int accessFlags = in.readUnsignedShort();
    int nameIndex = in.readUnsignedShort();
    int descriptorIndex = in.readUnsignedShort();

    String[] values = pool.getClassElement(ConstantPool.METHOD, clQualifiedName, nameIndex, descriptorIndex);

    Map<String, StructGeneralAttribute> attributes = readAttributes(in, pool);
    StructCodeAttribute code = (StructCodeAttribute)attributes.remove(StructGeneralAttribute.ATTRIBUTE_CODE.name);
    if (code != null) {
      attributes.putAll(code.codeAttributes);
    }

    return new StructMethod(accessFlags, attributes, values[0], values[1], bytecodeVersion, own ? code : null);
  }

  private static final int[] opr_iconst = {-1, 0, 1, 2, 3, 4, 5};
  private static final int[] opr_loadstore = {0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3};
  private static final int[] opcs_load = {CodeConstants.opc_iload, CodeConstants.opc_lload, CodeConstants.opc_fload, CodeConstants.opc_dload, CodeConstants.opc_aload};
  private static final int[] opcs_store = {CodeConstants.opc_istore, CodeConstants.opc_lstore, CodeConstants.opc_fstore, CodeConstants.opc_dstore, CodeConstants.opc_astore};

  private final String name;
  private final String descriptor;
  private final int bytecodeVersion;
  private final int localVariables;
  private final int codeLength;
  private final int codeFullLength;
  private InstructionSequence seq = null;
  private boolean expanded = false;

  private StructMethod(int accessFlags,
                       Map<String, StructGeneralAttribute> attributes,
                       String name,
                       String descriptor,
                       int bytecodeVersion,
                       StructCodeAttribute code) {
    super(accessFlags, attributes);
    this.name = name;
    this.descriptor = descriptor;
    this.bytecodeVersion = bytecodeVersion;
    if (code != null) {
      this.localVariables = code.localVariables;
      this.codeLength = code.codeLength;
      this.codeFullLength = code.codeFullLength;
    }
    else {
      this.localVariables = this.codeLength = this.codeFullLength = -1;
    }
  }

  public void expandData(StructClass classStruct) throws IOException {
    if (codeLength >= 0 && !expanded) {
      byte[] code = classStruct.getLoader().loadBytecode(classStruct, this, codeFullLength);
      seq = parseBytecode(new DataInputFullStream(code), codeLength, classStruct.getPool());
      expanded = true;
    }
  }

  public void releaseResources() {
    if (codeLength >= 0 && expanded) {
      seq = null;
      expanded = false;
    }
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  private InstructionSequence parseBytecode(DataInputFullStream in, int length, ConstantPool pool) throws IOException {
    VBStyleCollection<Instruction, Integer> instructions = new VBStyleCollection<>();

    for (int i = 0; i < length; ) {
      int offset = i;

      int opcode = in.readUnsignedByte();
      int group = CodeConstants.GROUP_GENERAL;

      boolean wide = (opcode == CodeConstants.opc_wide);

      if (wide) {
        i++;
        opcode = in.readUnsignedByte();
      }

      List<Integer> operands = new ArrayList<>();

      if (opcode >= CodeConstants.opc_iconst_m1 && opcode <= CodeConstants.opc_iconst_5) {
        operands.add(opr_iconst[opcode - CodeConstants.opc_iconst_m1]);
        opcode = CodeConstants.opc_bipush;
      }
      else if (opcode >= CodeConstants.opc_iload_0 && opcode <= CodeConstants.opc_aload_3) {
        operands.add(opr_loadstore[opcode - CodeConstants.opc_iload_0]);
        opcode = opcs_load[(opcode - CodeConstants.opc_iload_0) / 4];
      }
      else if (opcode >= CodeConstants.opc_istore_0 && opcode <= CodeConstants.opc_astore_3) {
        operands.add(opr_loadstore[opcode - CodeConstants.opc_istore_0]);
        opcode = opcs_store[(opcode - CodeConstants.opc_istore_0) / 4];
      }
      else {
        switch (opcode) {
          case CodeConstants.opc_bipush -> {
            operands.add((int)in.readByte());
            i++;
          }
          case CodeConstants.opc_ldc, CodeConstants.opc_newarray -> {
            operands.add(in.readUnsignedByte());
            i++;
          }
          case CodeConstants.opc_sipush, CodeConstants.opc_ifeq, CodeConstants.opc_ifne, CodeConstants.opc_iflt, CodeConstants.opc_ifge, CodeConstants.opc_ifgt, CodeConstants.opc_ifle, CodeConstants.opc_if_icmpeq, CodeConstants.opc_if_icmpne, CodeConstants.opc_if_icmplt,
            CodeConstants.opc_if_icmpge, CodeConstants.opc_if_icmpgt, CodeConstants.opc_if_icmple, CodeConstants.opc_if_acmpeq, CodeConstants.opc_if_acmpne, CodeConstants.opc_goto, CodeConstants.opc_jsr, CodeConstants.opc_ifnull, CodeConstants.opc_ifnonnull -> {
            if (opcode != CodeConstants.opc_sipush) {
              group = CodeConstants.GROUP_JUMP;
            }
            operands.add((int)in.readShort());
            i += 2;
          }
          case CodeConstants.opc_ldc_w, CodeConstants.opc_ldc2_w, CodeConstants.opc_getstatic, CodeConstants.opc_putstatic, CodeConstants.opc_getfield, CodeConstants.opc_putfield, CodeConstants.opc_invokevirtual, CodeConstants.opc_invokespecial,
            CodeConstants.opc_invokestatic, CodeConstants.opc_new, CodeConstants.opc_anewarray, CodeConstants.opc_checkcast, CodeConstants.opc_instanceof -> {
            operands.add(in.readUnsignedShort());
            i += 2;
            if (opcode >= CodeConstants.opc_getstatic && opcode <= CodeConstants.opc_putfield) {
              group = CodeConstants.GROUP_FIELDACCESS;
            }
            else if (opcode >= CodeConstants.opc_invokevirtual && opcode <= CodeConstants.opc_invokestatic) {
              group = CodeConstants.GROUP_INVOCATION;
            }
          }
          case CodeConstants.opc_invokedynamic -> {
            if (bytecodeVersion >= CodeConstants.BYTECODE_JAVA_7) { // instruction unused in Java 6 and before
              operands.add(in.readUnsignedShort());
              in.discard(2);
              group = CodeConstants.GROUP_INVOCATION;
              i += 4;
            }
          }
          case CodeConstants.opc_iload, CodeConstants.opc_lload, CodeConstants.opc_fload, CodeConstants.opc_dload, CodeConstants.opc_aload, CodeConstants.opc_istore, CodeConstants.opc_lstore,
            CodeConstants.opc_fstore, CodeConstants.opc_dstore, CodeConstants.opc_astore, CodeConstants.opc_ret -> {
            if (wide) {
              operands.add(in.readUnsignedShort());
              i += 2;
            }
            else {
              operands.add(in.readUnsignedByte());
              i++;
            }
            if (opcode == CodeConstants.opc_ret) {
              group = CodeConstants.GROUP_RETURN;
            }
          }
          case CodeConstants.opc_iinc -> {
            if (wide) {
              operands.add(in.readUnsignedShort());
              operands.add((int)in.readShort());
              i += 4;
            }
            else {
              operands.add(in.readUnsignedByte());
              operands.add((int)in.readByte());
              i += 2;
            }
          }
          case CodeConstants.opc_goto_w, CodeConstants.opc_jsr_w -> {
            opcode = opcode == CodeConstants.opc_jsr_w ? CodeConstants.opc_jsr : CodeConstants.opc_goto;
            operands.add(in.readInt());
            group = CodeConstants.GROUP_JUMP;
            i += 4;
          }
          case CodeConstants.opc_invokeinterface -> {
            operands.add(in.readUnsignedShort());
            operands.add(in.readUnsignedByte());
            in.discard(1);
            group = CodeConstants.GROUP_INVOCATION;
            i += 4;
          }
          case CodeConstants.opc_multianewarray -> {
            operands.add(in.readUnsignedShort());
            operands.add(in.readUnsignedByte());
            i += 3;
          }
          case CodeConstants.opc_tableswitch -> {
            in.discard((4 - (i + 1) % 4) % 4);
            i += ((4 - (i + 1) % 4) % 4); // padding
            operands.add(in.readInt());
            i += 4;
            int low = in.readInt();
            operands.add(low);
            i += 4;
            int high = in.readInt();
            operands.add(high);
            i += 4;

            for (int j = 0; j < high - low + 1; j++) {
              operands.add(in.readInt());
              i += 4;
            }
            group = CodeConstants.GROUP_SWITCH;
          }
          case CodeConstants.opc_lookupswitch -> {
            in.discard((4 - (i + 1) % 4) % 4);
            i += ((4 - (i + 1) % 4) % 4); // padding
            operands.add(in.readInt());
            i += 4;
            int npairs = in.readInt();
            operands.add(npairs);
            i += 4;

            for (int j = 0; j < npairs; j++) {
              operands.add(in.readInt());
              i += 4;
              operands.add(in.readInt());
              i += 4;
            }
            group = CodeConstants.GROUP_SWITCH;
          }
          case CodeConstants.opc_ireturn, CodeConstants.opc_lreturn, CodeConstants.opc_freturn, CodeConstants.opc_dreturn, CodeConstants.opc_areturn, CodeConstants.opc_return, CodeConstants.opc_athrow ->
            group = CodeConstants.GROUP_RETURN;
        }
      }

      int[] ops = null;
      if (!operands.isEmpty()) {
        ops = new int[operands.size()];
        for (int j = 0; j < operands.size(); j++) {
          ops[j] = operands.get(j);
        }
      }

      Instruction instr = Instruction.create(opcode, wide, group, bytecodeVersion, ops);
      instr.offsetFromMethodStart = instructions.size();
      instructions.addWithKey(instr, offset);

      i++;
    }

    // initialize exception table
    List<ExceptionHandler> lstHandlers = new ArrayList<>();

    int exception_count = in.readUnsignedShort();
    for (int i = 0; i < exception_count; i++) {
      ExceptionHandler handler = new ExceptionHandler();
      handler.from = in.readUnsignedShort();
      handler.to = in.readUnsignedShort();
      handler.handler = in.readUnsignedShort();

      int excclass = in.readUnsignedShort();
      if (excclass != 0) {
        handler.exceptionClass = pool.getPrimitiveConstant(excclass).getString();
      }

      lstHandlers.add(handler);
    }

    InstructionSequence seq = new FullInstructionSequence(instructions, new ExceptionTable(lstHandlers));

    // initialize instructions
    int i = seq.length() - 1;
    seq.setPointer(i);

    while (i >= 0) {
      Instruction instr = seq.getInstr(i--);
      if (instr.group != CodeConstants.GROUP_GENERAL) {
        instr.initInstruction(seq);
      }
      seq.addToPointer(-1);
    }

    return seq;
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public int getBytecodeVersion() {
    return bytecodeVersion;
  }

  public boolean containsCode() {
    return codeLength >= 0;
  }

  public int getLocalVariables() {
    return localVariables;
  }

  public InstructionSequence getInstructionSequence() {
    return seq;
  }

  public StructLocalVariableTableAttribute getLocalVariableAttr() {
    return getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TABLE);
  }

  @Override
  protected Type getType() {
    return MethodDescriptor.parseDescriptor(getDescriptor()).ret;
  }

  @Override
  public String toString() {
    return name;
  }
}
