// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.modules.decompiler.exps;

import me.f1nal.trinity.decompiler.output.impl.VariableOutputMember;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.ClassWriter;
import me.f1nal.trinity.decompiler.main.ClassesProcessor;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.rels.MethodWrapper;
import me.f1nal.trinity.decompiler.modules.decompiler.ExprProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;
import me.f1nal.trinity.decompiler.struct.attr.StructGeneralAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructLocalVariableTableAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructLocalVariableTypeTableAttribute;
import me.f1nal.trinity.decompiler.struct.gen.VarType;
import me.f1nal.trinity.decompiler.struct.gen.generics.GenericFieldDescriptor;
import me.f1nal.trinity.decompiler.struct.gen.generics.GenericMain;
import me.f1nal.trinity.decompiler.struct.match.IMatchable;
import me.f1nal.trinity.decompiler.struct.match.MatchEngine;
import me.f1nal.trinity.decompiler.struct.match.MatchNode;
import me.f1nal.trinity.decompiler.util.TextBuffer;
import me.f1nal.trinity.decompiler.util.TextUtil;
import me.f1nal.trinity.decompiler.struct.StructMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VarExprent extends Exprent {
  public static final int STACK_BASE = 20000;
  public static final String VAR_NAMELESS_ENCLOSURE = "<VAR_NAMELESS_ENCLOSURE>";

  private int index;
  private VarType varType;
  private boolean definition = false;
  private final VarProcessor processor;
  private final int visibleOffset;
  private int version = 0;
  private boolean classDef = false;
  private boolean stack = false;

  public VarExprent(int index, VarType varType, VarProcessor processor) {
    this(index, varType, processor, -1);
  }

  public VarExprent(int index, VarType varType, VarProcessor processor, int visibleOffset) {
    super(EXPRENT_VAR);
    this.index = index;
    this.varType = varType;
    this.processor = processor;
    this.visibleOffset = visibleOffset;
  }

  @Override
  public VarType getExprType() {
    return getVarType();
  }

  @Override
  public int getExprentUse() {
    return Exprent.MULTIPLE_USES | Exprent.SIDE_EFFECTS_FREE;
  }

  @Override
  public List<Exprent> getAllExprents() {
    return new ArrayList<>();
  }

  @Override
  public Exprent copy() {
    VarExprent var = new VarExprent(index, getVarType(), processor, visibleOffset);
    var.setDefinition(definition);
    var.setVersion(version);
    var.setClassDef(classDef);
    var.setStack(stack);
    return var;
  }

  @Override
  public TextBuffer toJava(int indent, BytecodeMappingTracer tracer) {
    TextBuffer buffer = new TextBuffer();

    tracer.addMapping(bytecode);

    if (classDef) {
      ClassesProcessor.ClassNode child = DecompilerContext.getClassProcessor().getMapRootClasses().get(varType.getValue());
      new ClassWriter().classToJava(child, buffer, indent, tracer);
      tracer.incrementCurrentSourceLine(buffer.countLines());
    }
    else {
      VarVersionPair varVersion = getVarVersionPair();
      String name = null;
      if (processor != null) {
        name = processor.getVarName(varVersion);
      }

      if (definition) {
        if (processor != null && processor.getVarFinal(varVersion) == VarProcessor.VAR_EXPLICIT_FINAL) {
          buffer.append(OutputMemberSerializer.keyword("final "));
        }
        appendDefinitionType(buffer);
        buffer.append(" ");
      }

      String varName = name == null ? ("var" + index + (this.version == 0 ? "" : "_" + this.version)) : name;
      buffer.append(OutputMemberSerializer.tag(varName, l -> new VariableOutputMember(l, varVersion.var, getVarType().getValue())));
    }

    return buffer;
  }

  public VarVersionPair getVarVersionPair() {
    return new VarVersionPair(index, version);
  }

  public String getDebugName(StructMethod method) {
    StructLocalVariableTableAttribute attr = method.getLocalVariableAttr();
    if (attr != null && processor != null) {
      Integer origIndex = processor.getVarOriginalIndex(index);
      if (origIndex != null) {
        String name = attr.getName(origIndex, visibleOffset);
        if (name != null && TextUtil.isValidIdentifier(name, method.getBytecodeVersion())) {
          return name;
        }
      }
    }
    return null;
  }

  private void appendDefinitionType(TextBuffer buffer) {
    if (DecompilerContext.getOption(IFernflowerPreferences.USE_DEBUG_VAR_NAMES)) {
      MethodWrapper method = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
      if (method != null) {
        Integer originalIndex = null;
        if (processor != null) {
          originalIndex = processor.getVarOriginalIndex(index);
        }
        if (originalIndex != null) {
          // first try from signature
          if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
            StructLocalVariableTypeTableAttribute attr =
              method.methodStruct.getAttribute(StructGeneralAttribute.ATTRIBUTE_LOCAL_VARIABLE_TYPE_TABLE);
            if (attr != null) {
              String signature = attr.getSignature(originalIndex, visibleOffset);
              if (signature != null) {
                GenericFieldDescriptor descriptor = GenericMain.parseFieldSignature(signature);
                if (descriptor != null) {
                  buffer.append(GenericMain.getGenericCastTypeName(descriptor.type, Collections.emptyList()));
                  return;
                }
              }
            }
          }

          // then try from descriptor
          StructLocalVariableTableAttribute attr = method.methodStruct.getLocalVariableAttr();
          if (attr != null) {
            String descriptor = attr.getDescriptor(originalIndex, visibleOffset);
            if (descriptor != null) {
              buffer.append(ExprProcessor.getCastTypeName(new VarType(descriptor), Collections.emptyList()));
              return;
            }
          }
        }
      }
    }

    VarType vt = getVarType();
    String castTypeName = ExprProcessor.getCastTypeName(vt, Collections.emptyList());
    buffer.append(castTypeName);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof VarExprent ve)) return false;

    return index == ve.getIndex() &&
           version == ve.getVersion() &&
           Objects.equals(getVarType(), ve.getVarType()); // FIXME: varType comparison redundant?
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public VarType getVarType() {
    VarType vt = null;
    if (processor != null) {
      vt = processor.getVarType(getVarVersionPair());
    }

    if (vt == null || (varType != null && varType.getType() != CodeConstants.TYPE_UNKNOWN)) {
      vt = varType;
    }

    return vt == null ? VarType.VARTYPE_UNKNOWN : vt;
  }

  public void setVarType(VarType varType) {
    this.varType = varType;
  }

  public boolean isDefinition() {
    return definition;
  }

  public void setDefinition(boolean definition) {
    this.definition = definition;
  }

  public VarProcessor getProcessor() {
    return processor;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public boolean isClassDef() {
    return classDef;
  }

  public void setClassDef(boolean classDef) {
    this.classDef = classDef;
  }

  public boolean isStack() {
    return stack;
  }

  public void setStack(boolean stack) {
    this.stack = stack;
  }

  // *****************************************************************************
  // IMatchable implementation
  // *****************************************************************************

  @Override
  public boolean match(MatchNode matchNode, MatchEngine engine) {
    if (!super.match(matchNode, engine)) {
      return false;
    }

    MatchNode.RuleValue rule = matchNode.getRules().get(IMatchable.MatchProperties.EXPRENT_VAR_INDEX);
    if (rule != null) {
      if (rule.isVariable()) {
        return engine.checkAndSetVariableValue((String)rule.value, this.index);
      }
      else {
        return this.index == Integer.parseInt((String)rule.value);
      }
    }

    return true;
  }
}