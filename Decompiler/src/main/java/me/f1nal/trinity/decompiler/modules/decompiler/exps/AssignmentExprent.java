// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.modules.decompiler.exps;

import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.ClassesProcessor;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.modules.decompiler.ExprProcessor;
import me.f1nal.trinity.decompiler.struct.gen.VarType;
import me.f1nal.trinity.decompiler.util.InterpreterUtil;
import me.f1nal.trinity.decompiler.util.TextBuffer;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.CheckTypesResult;
import me.f1nal.trinity.decompiler.struct.StructField;

import java.util.*;

public class AssignmentExprent extends Exprent {

  public static final int CONDITION_NONE = -1;

  private static final String[] OPERATORS = {
    " += ",   // FUNCTION_ADD
    " -= ",   // FUNCTION_SUB
    " *= ",   // FUNCTION_MUL
    " /= ",   // FUNCTION_DIV
    " &= ",   // FUNCTION_AND
    " |= ",   // FUNCTION_OR
    " ^= ",   // FUNCTION_XOR
    " %= ",   // FUNCTION_REM
    " <<= ",  // FUNCTION_SHL
    " >>= ",  // FUNCTION_SHR
    " >>>= "  // FUNCTION_USHR
  };

  private Exprent left;
  private Exprent right;
  private int condType = CONDITION_NONE;

  public AssignmentExprent(Exprent left, Exprent right, Set<Integer> bytecodeOffsets) {
    super(EXPRENT_ASSIGNMENT);
    this.left = left;
    this.right = right;

    addBytecodeOffsets(bytecodeOffsets);
  }

  @Override
  public VarType getExprType() {
    return left.getExprType();
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    VarType typeLeft = left.getExprType();
    VarType typeRight = right.getExprType();

    if (typeLeft.getTypeFamily() > typeRight.getTypeFamily()) {
      result.addMinTypeExprent(right, VarType.getMinTypeInFamily(typeLeft.getTypeFamily()));
    }
    else if (typeLeft.getTypeFamily() < typeRight.getTypeFamily()) {
      result.addMinTypeExprent(left, typeRight);
    }
    else {
      result.addMinTypeExprent(left, VarType.getCommonSupertype(typeLeft, typeRight));
    }

    return result;
  }

  @Override
  public List<Exprent> getAllExprents() {
    List<Exprent> lst = new ArrayList<>();
    lst.add(left);
    lst.add(right);
    return lst;
  }

  @Override
  public Exprent copy() {
    return new AssignmentExprent(left.copy(), right.copy(), bytecode);
  }

  @Override
  public int getPrecedence() {
    return 13;
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
    VarType leftType = left.getExprType();
    VarType rightType = right.getExprType();

    boolean fieldInClassInit = false, hiddenField = false;
    if (left.type == EXPRENT_FIELD) { // first assignment to a final field. Field name without "this" in front of it
      FieldExprent field = (FieldExprent)left;
      ClassesProcessor.ClassNode node = ((ClassesProcessor.ClassNode) DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE));
      if (node != null) {
        StructField fd = node.classStruct.getField(field.getName(), field.getDescriptor().descriptorString);
        if (fd != null) {
          if (field.isStatic() && fd.hasModifier(CodeConstants.ACC_FINAL)) {
            fieldInClassInit = true;
          }
          if (node.getWrapper() != null && node.getWrapper().getHiddenMembers().contains(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()))) {
            hiddenField = true;
          }
        }
      }
    }

    if (hiddenField) {
      return new TextBuffer();
    }

    TextBuffer buffer = new TextBuffer();

    if (fieldInClassInit) {
      buffer.append(((FieldExprent)left).getName());
    }
    else {
      buffer.append(left.toJava(indent, tracer));
    }

    if (right.type == EXPRENT_CONST) {
      ((ConstExprent) right).adjustConstType(leftType);
    }

    TextBuffer res = right.toJava(indent, tracer);

    if (condType == CONDITION_NONE &&
        !leftType.isSuperset(rightType) &&
        (rightType.equals(VarType.VARTYPE_OBJECT) || leftType.getType() != CodeConstants.TYPE_OBJECT)) {
      if (right.getPrecedence() >= FunctionExprent.getPrecedence(FunctionExprent.FUNCTION_CAST)) {
        res.enclose("(", ")");
      }

      res.prepend("(" + ExprProcessor.getCastTypeName(leftType, Collections.emptyList()) + ")");
    }

    buffer.append(condType == CONDITION_NONE ? " = " : OPERATORS[condType]).append(res);

    tracer.addMapping(bytecode);

    return buffer;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == left) {
      left = newExpr;
    }
    if (oldExpr == right) {
      right = newExpr;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof AssignmentExprent as)) return false;

    return Objects.equals(left, as.getLeft()) &&
           Objects.equals(right, as.getRight()) &&
           condType == as.getCondType();
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public Exprent getLeft() {
    return left;
  }

  public Exprent getRight() {
    return right;
  }

  public void setRight(Exprent right) {
    this.right = right;
  }

  public int getCondType() {
    return condType;
  }

  public void setCondType(int condType) {
    this.condType = condType;
  }
}
