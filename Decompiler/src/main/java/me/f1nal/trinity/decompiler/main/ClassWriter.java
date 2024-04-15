// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.main;

import me.f1nal.trinity.decompiler.output.impl.*;
import me.f1nal.trinity.decompiler.output.serialize.OutputMemberSerializer;
import me.f1nal.trinity.decompiler.struct.*;
import me.f1nal.trinity.decompiler.struct.attr.*;
import me.f1nal.trinity.decompiler.struct.gen.FieldDescriptor;
import me.f1nal.trinity.decompiler.struct.gen.MethodDescriptor;
import me.f1nal.trinity.decompiler.struct.gen.Type;
import me.f1nal.trinity.decompiler.struct.gen.VarType;
import me.f1nal.trinity.decompiler.struct.gen.generics.*;
import me.f1nal.trinity.decompiler.util.InterpreterUtil;
import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.collectors.BytecodeMappingTracer;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerLogger;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.rels.ClassWrapper;
import me.f1nal.trinity.decompiler.main.rels.MethodWrapper;
import me.f1nal.trinity.decompiler.modules.decompiler.ExprProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.AnnotationExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.ConstExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.NewExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.stats.RootStatement;
import me.f1nal.trinity.decompiler.modules.decompiler.typeann.TargetInfo;
import me.f1nal.trinity.decompiler.modules.decompiler.typeann.TypeAnnotation;
import me.f1nal.trinity.decompiler.modules.decompiler.typeann.TypeAnnotationWriteHelper;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;
import me.f1nal.trinity.decompiler.modules.renamer.PoolInterceptor;
import me.f1nal.trinity.decompiler.struct.consts.PrimitiveConstant;
import me.f1nal.trinity.decompiler.util.TextBuffer;

import java.util.*;
import java.util.stream.Collectors;

public class ClassWriter {
  private final PoolInterceptor interceptor;

  public ClassWriter() {
    interceptor = DecompilerContext.getPoolInterceptor();
  }

  private static void invokeProcessors(ClassesProcessor.ClassNode node) {
    ClassWrapper wrapper = node.getWrapper();
    StructClass cl = wrapper.getClassStruct();

    InitializerProcessor.extractInitializers(wrapper);

    if (node.type == ClassesProcessor.ClassNode.CLASS_ROOT &&
        !cl.isVersion5() &&
        DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_CLASS_1_4)) {
      ClassReference14Processor.processClassReferences(node);
    }

    if (cl.hasModifier(CodeConstants.ACC_ENUM) && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ENUM)) {
      EnumProcessor.clearEnum(wrapper);
    }

    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ASSERTIONS)) {
      AssertProcessor.buildAssertions(node);
    }
  }

  public void classLambdaToJava(ClassesProcessor.ClassNode node, TextBuffer buffer, Exprent method_object, int indent, BytecodeMappingTracer origTracer) {
    ClassWrapper wrapper = node.getWrapper();
    if (wrapper == null) {
      return;
    }

    boolean lambdaToAnonymous = DecompilerContext.getOption(IFernflowerPreferences.LAMBDA_TO_ANONYMOUS_CLASS);

    ClassesProcessor.ClassNode outerNode = (ClassesProcessor.ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_NODE, node);

    BytecodeMappingTracer tracer = new BytecodeMappingTracer(origTracer.getCurrentSourceLine());

    try {
      StructClass cl = wrapper.getClassStruct();

      DecompilerContext.getLogger().startWriteClass(node.simpleName);

      if (node.lambdaInformation.is_method_reference) {
        if (!node.lambdaInformation.is_content_method_static && method_object != null) {
          // reference to a virtual method
          buffer.append(method_object.toJava(indent, tracer));
        }
        else {
          // reference to a static method
          buffer.append(ExprProcessor.getCastTypeName(new VarType(node.lambdaInformation.content_class_name, true), Collections.emptyList()));
        }

        buffer.append("::")
          .append(OutputMemberSerializer.tag(CodeConstants.INIT_NAME.equals(node.lambdaInformation.content_method_name) ? "new" : node.lambdaInformation.content_method_name, l ->
                  new MethodOutputMember(l, node.lambdaInformation.content_class_name, node.lambdaInformation.content_method_name, node.lambdaInformation.content_method_descriptor)));
      }
      else {
        // lambda method
        StructMethod mt = cl.getMethod(node.lambdaInformation.content_method_key);
        MethodWrapper methodWrapper = wrapper.getMethodWrapper(mt.getName(), mt.getDescriptor());
        MethodDescriptor md_content = MethodDescriptor.parseDescriptor(node.lambdaInformation.content_method_descriptor);
        MethodDescriptor md_lambda = MethodDescriptor.parseDescriptor(node.lambdaInformation.method_descriptor);

        List<TypeAnnotation> parameterTypeAnnotations = TargetInfo.FormalParameterTarget.extract(TypeAnnotation.listFrom(mt));
        boolean explicitlyTyped = !parameterTypeAnnotations.isEmpty();

        if (!lambdaToAnonymous) {
          buffer.append('(');

          boolean firstParameter = true;
          int index = node.lambdaInformation.is_content_method_static ? 0 : 1;
          int start_index = md_content.params.length - md_lambda.params.length;

          for (int i = 0; i < md_content.params.length; i++) {
            if (i >= start_index) {
              if (!firstParameter) {
                buffer.append(", ");
              }

              if (explicitlyTyped) {
                List<TypeAnnotation> iParameterTypeAnnotations = TargetInfo.FormalParameterTarget.extract(parameterTypeAnnotations, i);
                VarType type = md_content.params[i];
                buffer.append(ExprProcessor.getCastTypeName(type, TypeAnnotationWriteHelper.create(iParameterTypeAnnotations)));
                buffer.append(' ');
              }

              String parameterName = methodWrapper.varproc.getVarName(new VarVersionPair(index, 0));
              buffer.append(parameterName == null ? "param" + index : parameterName); // null if decompiled with errors

              firstParameter = false;
            }

            index += md_content.params[i].getStackSize();
          }

          buffer.append(") ->");
        }

        buffer.append(" {").appendLineSeparator();
        tracer.incrementCurrentSourceLine();

        methodLambdaToJava(node, wrapper, mt, buffer, indent + 1, !lambdaToAnonymous, tracer);

        buffer.appendIndent(indent).append("}");

        addTracer(cl, mt, tracer);
      }
    }
    finally {
      DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_NODE, outerNode);
    }

    DecompilerContext.getLogger().endWriteClass();
  }

  public void classToJava(ClassesProcessor.ClassNode node, TextBuffer buffer, int indent, BytecodeMappingTracer tracer) {
    ClassesProcessor.ClassNode outerNode = (ClassesProcessor.ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_NODE, node);

    int startLine = tracer != null ? tracer.getCurrentSourceLine() : 0;
    BytecodeMappingTracer dummy_tracer = new BytecodeMappingTracer(startLine);

    try {
      // last minute processing
      invokeProcessors(node);

      ClassWrapper wrapper = node.getWrapper();
      StructClass cl = wrapper.getClassStruct();

      DecompilerContext.getLogger().startWriteClass(cl.qualifiedName);

      // write class definition
      int start_class_def = buffer.length();
      writeClassDefinition(node, buffer, indent);

      boolean hasContent = false;
      boolean enumFields = false;

      dummy_tracer.incrementCurrentSourceLine(buffer.countLines(start_class_def));

      List<StructRecordComponent> components = cl.getRecordComponents();

      for (StructField fd : cl.getFields()) {
        boolean hide = fd.isSynthetic() && DecompilerContext.getOption(IFernflowerPreferences.REMOVE_SYNTHETIC) ||
                       wrapper.getHiddenMembers().contains(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()));
        if (hide) continue;

        if (components != null && fd.getAccessFlags() == (CodeConstants.ACC_FINAL | CodeConstants.ACC_PRIVATE) &&
            components.stream().anyMatch(c -> c.getName().equals(fd.getName()) && c.getDescriptor().equals(fd.getDescriptor()))) {
          // Record component field: skip it
          continue;
        }

        boolean isEnum = fd.hasModifier(CodeConstants.ACC_ENUM) && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ENUM);
        if (isEnum) {
          if (enumFields) {
            buffer.append(',').appendLineSeparator();
            dummy_tracer.incrementCurrentSourceLine();
          }
          enumFields = true;
        }
        else if (enumFields) {
          buffer.append(';');
          buffer.appendLineSeparator();
          buffer.appendLineSeparator();
          dummy_tracer.incrementCurrentSourceLine(2);
          enumFields = false;
        }

        fieldToJava(wrapper, cl, fd, buffer, indent + 1, dummy_tracer); // FIXME: insert real tracer

        hasContent = true;
      }

      if (enumFields) {
        buffer.append(';').appendLineSeparator();
        dummy_tracer.incrementCurrentSourceLine();
      }

      // FIXME: fields don't matter at the moment
      startLine += buffer.countLines(start_class_def);

      // methods
      for (StructMethod mt : cl.getMethods()) {
        boolean hide = mt.isSynthetic() && DecompilerContext.getOption(IFernflowerPreferences.REMOVE_SYNTHETIC) ||
                       mt.hasModifier(CodeConstants.ACC_BRIDGE) && DecompilerContext.getOption(IFernflowerPreferences.REMOVE_BRIDGE) ||
                       wrapper.getHiddenMembers().contains(InterpreterUtil.makeUniqueKey(mt.getName(), mt.getDescriptor()));
        if (hide) continue;

        int position = buffer.length();
        int storedLine = startLine;
        if (hasContent) {
          buffer.appendLineSeparator();
          startLine++;
        }
        buffer.append(OutputMemberSerializer.serializeTags(new MethodStartEndOutputMember(0, node.classStruct.qualifiedName, mt.getDescriptor(), mt.getName())));
        BytecodeMappingTracer method_tracer = new BytecodeMappingTracer(startLine);
        boolean methodSkipped = !methodToJava(node, mt, buffer, indent + 1, method_tracer);
        buffer.append(OutputMemberSerializer.serializeTags(new MethodStartEndOutputMember(0)));
        if (!methodSkipped) {
          hasContent = true;
          addTracer(cl, mt, method_tracer);
          startLine = method_tracer.getCurrentSourceLine();
        }
        else {
          buffer.setLength(position);
          startLine = storedLine;
        }
      }

      // member classes
      for (ClassesProcessor.ClassNode inner : node.nested) {
        if (inner.type == ClassesProcessor.ClassNode.CLASS_MEMBER) {
          StructClass innerCl = inner.classStruct;
          boolean isSynthetic = (inner.access & CodeConstants.ACC_SYNTHETIC) != 0 || innerCl.isSynthetic();
          boolean hide = isSynthetic && DecompilerContext.getOption(IFernflowerPreferences.REMOVE_SYNTHETIC) ||
                         wrapper.getHiddenMembers().contains(innerCl.qualifiedName);
          if (hide) continue;

          if (hasContent) {
            buffer.appendLineSeparator();
            startLine++;
          }
          BytecodeMappingTracer class_tracer = new BytecodeMappingTracer(startLine);
          classToJava(inner, buffer, indent + 1, class_tracer);
          startLine = buffer.countLines();

          hasContent = true;
        }
      }

      buffer.appendIndent(indent).append('}');

      if (node.type != ClassesProcessor.ClassNode.CLASS_ANONYMOUS) {
        buffer.appendLineSeparator();
      }
    }
    finally {
      DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_NODE, outerNode);
    }

    DecompilerContext.getLogger().endWriteClass();
  }

  @SuppressWarnings("SpellCheckingInspection")
  private static boolean isSyntheticRecordMethod(StructClass cl, StructMethod mt, TextBuffer code) {
    if (cl.getRecordComponents() != null) {
      String name = mt.getName(), descriptor = mt.getDescriptor();
      if (name.equals("equals") && descriptor.equals("(Ljava/lang/Object;)Z") ||
          name.equals("hashCode") && descriptor.equals("()I") ||
          name.equals("toString") && descriptor.equals("()Ljava/lang/String;")) {
        if (code.countLines() == 1) {
          String str = code.toString().trim();
          return str.startsWith("return this." + name + "<invokedynamic>(this");
        }
      }
    }
    return false;
  }

  private void writeClassDefinition(ClassesProcessor.ClassNode node, TextBuffer buffer, int indent) {
    if (node.type == ClassesProcessor.ClassNode.CLASS_ANONYMOUS) {
      buffer.append(" {").appendLineSeparator();
      return;
    }

    ClassWrapper wrapper = node.getWrapper();
    StructClass cl = wrapper.getClassStruct();

    int flags = node.type == ClassesProcessor.ClassNode.CLASS_ROOT ? cl.getAccessFlags() : node.access;
    boolean isDeprecated = cl.hasAttribute(StructGeneralAttribute.ATTRIBUTE_DEPRECATED);
    boolean isSynthetic = (flags & CodeConstants.ACC_SYNTHETIC) != 0 || cl.hasAttribute(StructGeneralAttribute.ATTRIBUTE_SYNTHETIC);
    boolean isEnum = DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ENUM) && (flags & CodeConstants.ACC_ENUM) != 0;
    boolean isInterface = (flags & CodeConstants.ACC_INTERFACE) != 0;
    boolean isAnnotation = (flags & CodeConstants.ACC_ANNOTATION) != 0;
    boolean isAbstract = (flags & CodeConstants.ACC_ABSTRACT) != 0;

    if (isDeprecated) {
      appendDeprecation(buffer, indent);
    }

    if (interceptor != null) {
      String oldName = interceptor.getOldName(cl.qualifiedName);
      appendRenameComment(buffer, oldName, MType.CLASS, indent);
    }

    if (isSynthetic) {
      appendComment(buffer, "Class is synthetic.", indent);
    }

    appendAnnotations(buffer, indent, cl);

    buffer.appendIndent(indent);

    if (isEnum) {
      // remove abstract and final flags (JLS 8.9 Enums)
      flags &= ~CodeConstants.ACC_ABSTRACT;
      flags &= ~CodeConstants.ACC_FINAL;
    }

    List<StructRecordComponent> components = cl.getRecordComponents();
    List<String> permittedSubclassQualifiedNames = cl.getPermittedSubclasses();

    if (components != null) {
      // records are implicitly final
      flags &= ~CodeConstants.ACC_FINAL;
    }

    appendModifiers(buffer, flags, CLASS_ALLOWED, isInterface, CLASS_EXCLUDED);

    if (permittedSubclassQualifiedNames != null && !isEnum) {
      buffer.append("sealed ");
    }
    else if (node.isNonSealed()) {
      buffer.append("non-sealed ");
    }

    if (isEnum) {
      buffer.append(OutputMemberSerializer.kind("enum", KindOutputMember.KindType.CLASS_ENUM) + " ");
    }
    else if (isInterface) {
      if (isAnnotation) {
        buffer.append(OutputMemberSerializer.kind("@interface", KindOutputMember.KindType.CLASS_ANNOTATION));
      } else {
        buffer.append(OutputMemberSerializer.kind("interface", KindOutputMember.KindType.CLASS_INTERFACE));
      }
      buffer.append(' ');
    }
    else if (components != null) {
      buffer.append("record ");
    }
    else {
      buffer.append(OutputMemberSerializer.kind("class", isAbstract ? KindOutputMember.KindType.CLASS_ABSTRACT : KindOutputMember.KindType.CLASS_CLASSES));
      buffer.append(' ');
    }
    buffer.append(OutputMemberSerializer.serializeTags(new ClassOutputMember(node.simpleName.length(), cl.qualifiedName)));
    buffer.append(node.simpleName);

    List<TypeAnnotation> typeAnnotations = TypeAnnotation.listFrom(cl);

    GenericClassDescriptor descriptor = getGenericClassDescriptor(cl);
    if (descriptor != null && !descriptor.fparameters.isEmpty()) {
      appendTypeParameters(buffer, descriptor.fparameters, descriptor.fbounds, typeAnnotations);
    }

    if (components != null) {
      buffer.append('(');
      for (int i = 0; i < components.size(); i++) {
        StructRecordComponent cd = components.get(i);
        if (i > 0) {
          buffer.append(", ");
        }
        boolean varArgComponent = i == components.size() - 1 && isVarArgRecord(cl);
        recordComponentToJava(cd, buffer, varArgComponent);
      }
      buffer.append(')');
    }

    buffer.append(' ');

    if (!isEnum && !isInterface && components == null && cl.superClass != null) {
      VarType supertype = new VarType(cl.superClass.getString(), true);
      List<TypeAnnotation> extendsTypeAnnotations = TargetInfo.SupertypeTarget.extractExtends(typeAnnotations);
      if (!VarType.VARTYPE_OBJECT.equals(supertype)) {
        buffer.append(OutputMemberSerializer.keyword("extends "));
        if (descriptor != null) {
          buffer.append(GenericMain.getGenericCastTypeName(
            descriptor.superclass,
            TypeAnnotationWriteHelper.create(extendsTypeAnnotations))
          );
        }
        else {
          buffer.append(ExprProcessor.getCastTypeName(supertype, TypeAnnotationWriteHelper.create(extendsTypeAnnotations)));
        }
        buffer.append(' ');
      }
    }

    if (!isAnnotation) {
      int[] interfaces = cl.getInterfaces();
      if (interfaces.length > 0) {
        buffer.append(OutputMemberSerializer.keyword(isInterface ? "extends " : "implements "));
        for (int i = 0; i < interfaces.length; i++) {
          if (i > 0) {
            buffer.append(", ");
          }
          int finalI = i;
          List<TypeAnnotation> superTypeAnnotations = TargetInfo.SupertypeTarget.extract(typeAnnotations, i);
          if (descriptor != null) {
            buffer.append(GenericMain.getGenericCastTypeName(
              descriptor.superinterfaces.get(i),
              TypeAnnotationWriteHelper.create(superTypeAnnotations)
            ));
          }
          else {
            buffer.append(ExprProcessor.getCastTypeName(
              new VarType(cl.getInterface(i), true),
              TypeAnnotationWriteHelper.create(superTypeAnnotations)
            ));
          }
        }
        buffer.append(' ');
      }
    }

    if (permittedSubclassQualifiedNames != null && !permittedSubclassQualifiedNames.isEmpty()) {
      Set<String> qualifiedNested = node.nested.stream()
        .map(nestedNode -> nestedNode.classStruct.qualifiedName)
        .collect(Collectors.toSet());
      boolean allSubClassesAreNested = qualifiedNested.containsAll(permittedSubclassQualifiedNames);
      if (!allSubClassesAreNested) { // only generate permits lists for non-nested classes
        buffer.append(OutputMemberSerializer.keyword("permits "));
        for (int i = 0; i < permittedSubclassQualifiedNames.size(); i++) {
          String qualifiedName = permittedSubclassQualifiedNames.get(i);
          if (i > 0) {
            buffer.append(", ");
          }
          String nestedName = DecompilerContext.getImportCollector().getNestedName(qualifiedName);
          buffer.append(nestedName);
        }
        buffer.append(' ');
      }
    }
    buffer.append('{').appendLineSeparator();
  }

  private void fieldToJava(ClassWrapper wrapper, StructClass cl, StructField fd, TextBuffer buffer, int indent, BytecodeMappingTracer tracer) {
    int start = buffer.length();
    boolean isInterface = cl.hasModifier(CodeConstants.ACC_INTERFACE);
    boolean isDeprecated = fd.hasAttribute(StructGeneralAttribute.ATTRIBUTE_DEPRECATED);
    boolean isEnum = fd.hasModifier(CodeConstants.ACC_ENUM) && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ENUM);

    if (isDeprecated) {
      appendDeprecation(buffer, indent);
    }

    if (interceptor != null) {
      String oldName = interceptor.getOldName(cl.qualifiedName + " " + fd.getName() + " " + fd.getDescriptor());
      appendRenameComment(buffer, oldName, MType.FIELD, indent);
    }

    if (fd.isSynthetic()) {
      appendComment(buffer, "Field is synthetic.", indent);
    }

    Map.Entry<VarType, GenericFieldDescriptor> fieldTypeData = getFieldTypeData(fd);
    VarType fieldType = fieldTypeData.getKey();

    appendAnnotations(buffer, indent, fd);

    buffer.appendIndent(indent);
    buffer.append(OutputMemberSerializer.serializeTags(new FieldDeclarationOutputMember(0, cl.qualifiedName, fd.getDescriptor(), fd.getName())));

    if (!isEnum) {
      appendModifiers(buffer, fd.getAccessFlags(), FIELD_ALLOWED, isInterface, FIELD_EXCLUDED);
    }

    GenericFieldDescriptor descriptor = fieldTypeData.getValue();

    final List<TypeAnnotation> typeAnnotations = TypeAnnotation.listFrom(fd);

    if (!isEnum) {
      if (descriptor != null) {
        buffer.append(GenericMain.getGenericCastTypeName(descriptor.type, TypeAnnotationWriteHelper.create(typeAnnotations)));
      }
      else {
        buffer.append(ExprProcessor.getCastTypeName(fieldType, TypeAnnotationWriteHelper.create(typeAnnotations)));
      }
      buffer.append(' ');
    }

    Exprent initializer;
    if (fd.hasModifier(CodeConstants.ACC_STATIC)) {
      initializer = wrapper.getStaticFieldInitializers().getWithKey(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()));
    }
    else {
      initializer = wrapper.getDynamicFieldInitializers().getWithKey(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()));
    }

    String name = Objects.requireNonNullElse(fd.getName(), "null");
    String displayName = name;

/*    if (isEnum && initializer != null && initializer.type == Exprent.EXPRENT_NEW) {
      NewExprent expr = (NewExprent) initializer;
      if (expr.getConstructor() != null && !expr.getConstructor().getParameters().isEmpty()) {
        Exprent exprent = expr.getConstructor().getParameters().get(0);
        if (exprent instanceof ConstExprent && ((ConstExprent) exprent).getConstType().equals(VarType.VARTYPE_STRING)) {
          displayName = (String) ((ConstExprent) exprent).getValue();
        }
      }
    }*/
    buffer.append(OutputMemberSerializer.serializeTags(new FieldOutputMember(displayName.length(), cl.qualifiedName, name, fd.getDescriptor())));
    buffer.append(displayName);

    tracer.incrementCurrentSourceLine(buffer.countLines(start));
    if (initializer != null) {
      if (isEnum && initializer.type == Exprent.EXPRENT_NEW) {
        NewExprent expr = (NewExprent)initializer;
        expr.setEnumConst(true);
        buffer.append(expr.toJava(indent, tracer));
      }
      else {
        buffer.append(" = ");

        if (initializer.type == Exprent.EXPRENT_CONST) {
          ((ConstExprent) initializer).adjustConstType(fieldType);
        }

        // FIXME: special case field initializer. Can map to more than one method (constructor) and bytecode instruction
        buffer.append(initializer.toJava(indent, tracer));
      }
    }
    else if (fd.hasModifier(CodeConstants.ACC_FINAL) && fd.hasModifier(CodeConstants.ACC_STATIC)) {
      StructConstantValueAttribute attr = fd.getAttribute(StructGeneralAttribute.ATTRIBUTE_CONSTANT_VALUE);
      if (attr != null) {
        PrimitiveConstant constant = cl.getPool().getPrimitiveConstant(attr.getIndex());
        buffer.append(" = ");
        buffer.append(new ConstExprent(fieldType, constant.value, null, fd).toJava(indent, tracer));
      }
    }

    if (!isEnum) {
      buffer.append(';').appendLineSeparator();
      tracer.incrementCurrentSourceLine();
    }
  }

  private static void writeModuleInfoBody(TextBuffer buffer, StructModuleAttribute moduleAttribute) {
    boolean newLineNeeded = false;

    List<StructModuleAttribute.RequiresEntry> requiresEntries = moduleAttribute.requires;
    if (!requiresEntries.isEmpty()) {
      for (StructModuleAttribute.RequiresEntry requires : requiresEntries) {
        if (!isGenerated(requires.flags)) {
          buffer.appendIndent(1).append("requires ");
          if ((requires.flags & CodeConstants.ACC_STATIC_PHASE) != 0) buffer.append("static ");
          if ((requires.flags & CodeConstants.ACC_TRANSITIVE) != 0) buffer.append("transitive ");
          buffer.append(requires.moduleName.replace('/', '.')).append(';').appendLineSeparator();
          newLineNeeded = true;
        }
      }
    }

    List<StructModuleAttribute.ExportsEntry> exportsEntries = moduleAttribute.exports;
    if (!exportsEntries.isEmpty()) {
      if (newLineNeeded) buffer.appendLineSeparator();
      for (StructModuleAttribute.ExportsEntry exports : exportsEntries) {
        if (!isGenerated(exports.flags)) {
          buffer.appendIndent(1).append("exports ").append(exports.packageName.replace('/', '.'));
          List<String> exportToModules = exports.exportToModules;
          if (!exportToModules.isEmpty()) {
            buffer.append(" to").appendLineSeparator();
            appendFQClassNames(buffer, exportToModules);
          }
          buffer.append(';').appendLineSeparator();
          newLineNeeded = true;
        }
      }
    }

    List<StructModuleAttribute.OpensEntry> opensEntries = moduleAttribute.opens;
    if (!opensEntries.isEmpty()) {
      if (newLineNeeded) buffer.appendLineSeparator();
      for (StructModuleAttribute.OpensEntry opens : opensEntries) {
        if (!isGenerated(opens.flags)) {
          buffer.appendIndent(1).append("opens ").append(opens.packageName.replace('/', '.'));
          List<String> opensToModules = opens.opensToModules;
          if (!opensToModules.isEmpty()) {
            buffer.append(" to").appendLineSeparator();
            appendFQClassNames(buffer, opensToModules);
          }
          buffer.append(';').appendLineSeparator();
          newLineNeeded = true;
        }
      }
    }

    List<String> usesEntries = moduleAttribute.uses;
    if (!usesEntries.isEmpty()) {
      if (newLineNeeded) buffer.appendLineSeparator();
      for (String uses : usesEntries) {
        buffer.appendIndent(1).append("uses ").append(ExprProcessor.buildJavaClassName(uses)).append(';').appendLineSeparator();
      }
      newLineNeeded = true;
    }

    List<StructModuleAttribute.ProvidesEntry> providesEntries = moduleAttribute.provides;
    if (!providesEntries.isEmpty()) {
      if (newLineNeeded) buffer.appendLineSeparator();
      for (StructModuleAttribute.ProvidesEntry provides : providesEntries) {
        buffer.appendIndent(1).append("provides ").append(ExprProcessor.buildJavaClassName(provides.interfaceName)).append(" with").appendLineSeparator();
        appendFQClassNames(buffer, provides.implementationNames.stream().map(ExprProcessor::buildJavaClassName).collect(Collectors.toList()));
        buffer.append(';').appendLineSeparator();
      }
    }
  }

  private static boolean isGenerated(int flags) {
    return (flags & (CodeConstants.ACC_SYNTHETIC | CodeConstants.ACC_MANDATED)) != 0;
  }

  private static void addTracer(StructClass cls, StructMethod method, BytecodeMappingTracer tracer) {
    StructLineNumberTableAttribute table = method.getAttribute(StructGeneralAttribute.ATTRIBUTE_LINE_NUMBER_TABLE);
    tracer.setLineNumberTable(table);
    String key = InterpreterUtil.makeUniqueKey(method.getName(), method.getDescriptor());
    DecompilerContext.getBytecodeSourceMapper().addTracer(cls.qualifiedName, key, tracer);
  }

  private boolean methodToJava(ClassesProcessor.ClassNode node, StructMethod mt, TextBuffer buffer, int indent, BytecodeMappingTracer tracer) {
    ClassWrapper wrapper = node.getWrapper();
    StructClass cl = wrapper.getClassStruct();
    MethodWrapper methodWrapper = wrapper.getMethodWrapper(mt.getName(), mt.getDescriptor());

    boolean hideMethod = false;
    int start_index_method = buffer.length();

    MethodWrapper outerWrapper = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD_WRAPPER, methodWrapper);

    try {
      boolean isInterface = cl.hasModifier(CodeConstants.ACC_INTERFACE);
      boolean isAnnotation = cl.hasModifier(CodeConstants.ACC_ANNOTATION);
      boolean isDeprecated = mt.hasAttribute(StructGeneralAttribute.ATTRIBUTE_DEPRECATED);
      boolean clInit = false, dInit = false;

      MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());

      int flags = mt.getAccessFlags();
      if ((flags & CodeConstants.ACC_NATIVE) != 0) {
        flags &= ~CodeConstants.ACC_STRICT; // compiler bug: a strictfp class sets all methods to strictfp
      }
      if (CodeConstants.CLINIT_NAME.equals(mt.getName())) {
        flags &= CodeConstants.ACC_STATIC; // ignore all modifiers except 'static' in a static initializer
      }

      if (isDeprecated) {
        appendDeprecation(buffer, indent);
      }

      if (interceptor != null) {
        String oldName = interceptor.getOldName(cl.qualifiedName + " " + mt.getName() + " " + mt.getDescriptor());
        appendRenameComment(buffer, oldName, MType.METHOD, indent);
      }

      boolean isSynthetic = (flags & CodeConstants.ACC_SYNTHETIC) != 0 || mt.hasAttribute(StructGeneralAttribute.ATTRIBUTE_SYNTHETIC);
      boolean isBridge = (flags & CodeConstants.ACC_BRIDGE) != 0;
      List<String> comments = new ArrayList<>();
      if (isSynthetic) {
        comments.add("synthetic");
      }
      if (isBridge) {
        comments.add("bridged");
      }
      if (!comments.isEmpty()) {
        appendComment(buffer, String.format("Method is %s.", String.join(", ", comments)), indent);
      }

      GenericMethodDescriptor descriptor = null;
      if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
        StructGenericSignatureAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_SIGNATURE);
        if (attr != null) {
          descriptor = GenericMain.parseMethodSignature(attr.getSignature());
          if (descriptor != null) {
            long actualParams = md.params.length;
            List<VarVersionPair> mask = methodWrapper.synthParameters;
            if (mask != null) {
              actualParams = mask.stream().filter(Objects::isNull).count();
            }
            if (actualParams != descriptor.parameterTypes.size()) {
              String message = "Inconsistent generic signature in method " + mt.getName() + " " + mt.getDescriptor() + " in " + cl.qualifiedName;
              DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
              descriptor = null;
            }
          }
        }
      }
      appendAnnotations(buffer, indent, mt);

      buffer.appendIndent(indent);

      appendModifiers(buffer, flags, METHOD_ALLOWED, isInterface, METHOD_EXCLUDED);

      if (isInterface && !mt.hasModifier(CodeConstants.ACC_STATIC) && !mt.hasModifier(CodeConstants.ACC_PRIVATE) && mt.containsCode()) {
        // 'default' modifier (Java 8)
        buffer.append("default ");
      }

      String name = mt.getName();
      boolean init = false;
      if (CodeConstants.INIT_NAME.equals(name)) {
        if (node.type == ClassesProcessor.ClassNode.CLASS_ANONYMOUS) {
          name = "";
          dInit = true;
        }
        else {
          name = node.simpleName;
          init = true;
        }
      }
      else if (CodeConstants.CLINIT_NAME.equals(name)) {
        name = "";
        clInit = true;
      }

      boolean throwsExceptions = false;
      int paramCount = 0;
      final List<TypeAnnotation> typeAnnotations = TypeAnnotation.listFrom(mt);
      if (!clInit && !dInit) {
        if (descriptor != null && !descriptor.typeParameters.isEmpty()) {
          appendTypeParameters(buffer, descriptor.typeParameters, descriptor.typeParameterBounds, typeAnnotations);
          buffer.append(' ');
        }

        final List<TypeAnnotation> emptyTypeAnnotations = TargetInfo.EmptyTarget.extract(typeAnnotations);
        if (init) {
          emptyTypeAnnotations.forEach(typeAnnotation -> typeAnnotation.writeTo(buffer));
        } else {
          if (descriptor != null) {
            buffer.append(GenericMain.getGenericCastTypeName(descriptor.returnType, TypeAnnotationWriteHelper.create(emptyTypeAnnotations)));
          }
          else {
            buffer.append(ExprProcessor.getCastTypeName(md.ret, TypeAnnotationWriteHelper.create(emptyTypeAnnotations)));
          }
          buffer.append(' ');
        }

        String validJavaIdentifier = toValidJavaIdentifier(name);
        MethodOutputMember outputMember = new MethodOutputMember(validJavaIdentifier.length(), cl.qualifiedName, mt.getName(), mt.getDescriptor());
        buffer.append(OutputMemberSerializer.serializeTags(outputMember) + validJavaIdentifier);
        buffer.append('(');

        List<VarVersionPair> mask = methodWrapper.synthParameters;

        int lastVisibleParameterIndex = -1;
        for (int i = 0; i < md.params.length; i++) {
          if (mask == null || mask.get(i) == null) {
            lastVisibleParameterIndex = i;
          }
        }

        int index = methodWrapper.varproc.getFirstParameterVarIndex();
        for (int i = methodWrapper.varproc.getFirstParameterPosition(); i < md.params.length; i++) {
          if (mask == null || mask.get(i) == null) {
            if (paramCount > 0) {
              buffer.append(", ");
            }

            Type paramType;
            if (descriptor != null) paramType = descriptor.parameterTypes.get(paramCount); else paramType = md.params[i];
            appendParameterAnnotations(buffer, mt, paramType, paramCount);

            VarVersionPair pair = new VarVersionPair(index, 0);
            if (methodWrapper.varproc.isParameterFinal(pair) ||
                methodWrapper.varproc.getVarFinal(pair) == VarProcessor.VAR_EXPLICIT_FINAL) {
              buffer.append("final ");
            }

            String typeName;
            boolean isVarArg = i == lastVisibleParameterIndex && mt.hasModifier(CodeConstants.ACC_VARARGS);
            List<TypeAnnotation> typeParamAnnotations = TargetInfo.FormalParameterTarget.extract(typeAnnotations, i);
            if (paramType instanceof GenericType genParamType) {
              isVarArg &= genParamType.getArrayDim() > 0;
              if (isVarArg) {
                genParamType = genParamType.decreaseArrayDim();
              }
              typeName = GenericMain.getGenericCastTypeName(genParamType, TypeAnnotationWriteHelper.create(typeParamAnnotations));
            }
            else {
              VarType varParamType = (VarType) paramType;
              isVarArg &= varParamType.getArrayDim() > 0;
              if (isVarArg) {
                varParamType = varParamType.decreaseArrayDim();
              }
              typeName = ExprProcessor.getCastTypeName(varParamType, TypeAnnotationWriteHelper.create(typeParamAnnotations));
            }

            if (ExprProcessor.UNDEFINED_TYPE_STRING.equals(typeName) &&
                DecompilerContext.getOption(IFernflowerPreferences.UNDEFINED_PARAM_TYPE_OBJECT)) {
              typeName = ExprProcessor.getCastTypeName(VarType.VARTYPE_OBJECT, TypeAnnotationWriteHelper.create(typeParamAnnotations));
            }
            buffer.append(typeName);
            if (isVarArg) {
              buffer.append("...");
            }

            buffer.append(' ');

            String parameterName = methodWrapper.varproc.getVarName(pair);
            if (parameterName == null) parameterName = "param" + index; // null iff decompiled with errors
            buffer.append(OutputMemberSerializer.serializeTags(new VariableOutputMember(parameterName.length(), pair.var, paramType.getValue())));
            buffer.append(parameterName);

            paramCount++;
          }

          index += md.params[i].getStackSize();
        }
        buffer.append(')');

        StructExceptionsAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_EXCEPTIONS);
        if ((descriptor != null && !descriptor.exceptionTypes.isEmpty()) || attr != null) {
          throwsExceptions = true;
          buffer.append(" throws ");

          for (int i = 0; i < attr.getThrowsExceptions().size(); i++) {
            if (i > 0) {
              buffer.append(", ");
            }
            TargetInfo.ThrowsTarget.extract(typeAnnotations, i).forEach(typeAnnotation -> typeAnnotation.writeTo(buffer));
            if (descriptor != null && !descriptor.exceptionTypes.isEmpty()) {
              GenericType type = descriptor.exceptionTypes.get(i);
              buffer.append(GenericMain.getGenericCastTypeName(type, Collections.emptyList()));
            }
            else {
              VarType type = new VarType(attr.getExcClassname(i, cl.getPool()), true);
              buffer.append(ExprProcessor.getCastTypeName(type, Collections.emptyList()));
            }
          }
        }
      }

      tracer.incrementCurrentSourceLine(buffer.countLines(start_index_method));

      if ((flags & (CodeConstants.ACC_ABSTRACT | CodeConstants.ACC_NATIVE)) != 0) { // native or abstract method (explicit or interface)
        if (isAnnotation) {
          StructAnnDefaultAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_ANNOTATION_DEFAULT);
          if (attr != null) {
            buffer.append(" default ");
            buffer.append(attr.getDefaultValue().toJava(0, BytecodeMappingTracer.DUMMY));
          }
        }

        buffer.append(';');
        buffer.appendLineSeparator();
      }
      else {
        if (!clInit && !dInit) {
          buffer.append(' ');
        }

        // We do not have line information for method start, lets have it here for now
        if (CodeConstants.CLINIT_NAME.equals(mt.getName())) {
          MethodOutputMember outputMember = new MethodOutputMember("{".length(), cl.qualifiedName, mt.getName(), mt.getDescriptor());
          buffer.append(OutputMemberSerializer.serializeTags(outputMember));
        }
        buffer.append('{');
        buffer.appendLineSeparator();
        tracer.incrementCurrentSourceLine();

        RootStatement root = wrapper.getMethodWrapper(mt.getName(), mt.getDescriptor()).root;

        if (root != null && !methodWrapper.decompiledWithErrors) { // check for existence
          try {
            // to restore in case of an exception
            BytecodeMappingTracer codeTracer = new BytecodeMappingTracer(tracer.getCurrentSourceLine());
            TextBuffer code = root.toJava(indent + 1, codeTracer);

            hideMethod = code.length() == 0 &&
              (clInit || dInit || hideConstructor(node, !typeAnnotations.isEmpty(), init, throwsExceptions, paramCount, flags)) ||
              isSyntheticRecordMethod(cl, mt, code);

            buffer.append(code);

            tracer.setCurrentSourceLine(codeTracer.getCurrentSourceLine());
            tracer.addTracer(codeTracer);
          }
          catch (Throwable t) {
            String message = "Method " + mt.getName() + " " + mt.getDescriptor() + " couldn't be written.";
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN, t);
            methodWrapper.setErrorStacktrace(t);
            methodWrapper.decompiledWithErrors = true;
          }
        }

        if (methodWrapper.decompiledWithErrors) {
          buffer.appendIndent(indent + 1);
          buffer.append("// Fatal error while decompiling.");
          if (methodWrapper.errorStacktrace != null) {
            for (String line : methodWrapper.errorStacktrace.split("\n")) {
              buffer.appendLineSeparator();
              buffer.appendIndent(indent + 1);
              buffer.append("// " + line);
            }
          } else {
            buffer.appendLineSeparator();
            buffer.appendIndent(indent + 1);
            buffer.append("// No stacktrace available!");
          }
          buffer.appendLineSeparator();
          tracer.incrementCurrentSourceLine();
        }
        else if (root != null) {
          tracer.addMapping(root.getDummyExit().bytecode);
        }
        buffer.appendIndent(indent).append('}').appendLineSeparator();
      }

      tracer.incrementCurrentSourceLine();
    }
    finally {
      DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD_WRAPPER, outerWrapper);
    }

    // save total lines
    // TODO: optimize
    //tracer.setCurrentSourceLine(buffer.countLines(start_index_method));

    return !hideMethod;
  }

  private static boolean isVarArgRecord(StructClass cl) {
    String canonicalConstructorDescriptor =
      cl.getRecordComponents().stream().map(StructField::getDescriptor).collect(Collectors.joining("", "(", ")V"));
    StructMethod init = cl.getMethod(CodeConstants.INIT_NAME, canonicalConstructorDescriptor);
    return init != null && init.hasModifier(CodeConstants.ACC_VARARGS);
  }

  public static void packageInfoToJava(StructClass cl, TextBuffer buffer) {
    appendAnnotations(buffer, 0, cl);

    int index = cl.qualifiedName.lastIndexOf('/');
    String packageName = cl.qualifiedName.substring(0, index).replace('/', '.');
    buffer.append(OutputMemberSerializer.keyword("package ")).append(packageName).append(';').appendLineSeparator().appendLineSeparator();
  }

  public static void moduleInfoToJava(StructClass cl, TextBuffer buffer) {
    appendAnnotations(buffer, 0, cl);

    StructModuleAttribute moduleAttribute = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_MODULE);

    if ((moduleAttribute.moduleFlags & CodeConstants.ACC_OPEN) != 0) {
      buffer.append("open ");
    }

    buffer.append("module ").append(moduleAttribute.moduleName).append(" {").appendLineSeparator();

    writeModuleInfoBody(buffer, moduleAttribute);

    buffer.append('}').appendLineSeparator();
  }

  private static void methodLambdaToJava(ClassesProcessor.ClassNode lambdaNode,
                                         ClassWrapper classWrapper,
                                         StructMethod mt,
                                         TextBuffer buffer,
                                         int indent,
                                         boolean codeOnly, BytecodeMappingTracer tracer) {
    MethodWrapper methodWrapper = classWrapper.getMethodWrapper(mt.getName(), mt.getDescriptor());

    MethodWrapper outerWrapper = (MethodWrapper)DecompilerContext.getProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD_WRAPPER, methodWrapper);

    try {
      String method_name = lambdaNode.lambdaInformation.method_name;
      MethodDescriptor md_content = MethodDescriptor.parseDescriptor(lambdaNode.lambdaInformation.content_method_descriptor);
      MethodDescriptor md_lambda = MethodDescriptor.parseDescriptor(lambdaNode.lambdaInformation.method_descriptor);

      if (!codeOnly) {
        buffer.appendIndent(indent);
        buffer.append("public ");
        buffer.append(method_name);
        buffer.append("(");

        boolean firstParameter = true;
        int index = lambdaNode.lambdaInformation.is_content_method_static ? 0 : 1;
        int start_index = md_content.params.length - md_lambda.params.length;

        for (int i = 0; i < md_content.params.length; i++) {
          if (i >= start_index) {
            if (!firstParameter) {
              buffer.append(", ");
            }

            String typeName = ExprProcessor.getCastTypeName(md_content.params[i].copy(), Collections.emptyList());
            if (ExprProcessor.UNDEFINED_TYPE_STRING.equals(typeName) &&
                DecompilerContext.getOption(IFernflowerPreferences.UNDEFINED_PARAM_TYPE_OBJECT)) {
              typeName = ExprProcessor.getCastTypeName(VarType.VARTYPE_OBJECT, Collections.emptyList());
            }

            buffer.append(typeName);
            buffer.append(" ");

            String parameterName = methodWrapper.varproc.getVarName(new VarVersionPair(index, 0));
            buffer.append(parameterName == null ? "param" + index : parameterName); // null iff decompiled with errors

            firstParameter = false;
          }

          index += md_content.params[i].getStackSize();
        }

        buffer.append(") {").appendLineSeparator();

        indent += 1;
      }

      RootStatement root = classWrapper.getMethodWrapper(mt.getName(), mt.getDescriptor()).root;
      if (!methodWrapper.decompiledWithErrors) {
        if (root != null) { // check for existence
          try {
            buffer.append(root.toJava(indent, tracer));
          }
          catch (Throwable t) {
            String message = "Method " + mt.getName() + " " + mt.getDescriptor() + " couldn't be written.";
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN, t);
            methodWrapper.decompiledWithErrors = true;
          }
        }
      }

      if (methodWrapper.decompiledWithErrors) {
        TextBuffer commentBuffer = new TextBuffer();
        commentBuffer.appendIndent(indent);
        commentBuffer.append("// Failed to decompile!");
        commentBuffer.appendLineSeparator();
        buffer.append(OutputMemberSerializer.tag(commentBuffer.toString(), CommentOutputMember::new));
      }

      if (root != null) {
        tracer.addMapping(root.getDummyExit().bytecode);
      }

      if (!codeOnly) {
        indent -= 1;
        buffer.appendIndent(indent).append('}').appendLineSeparator();
      }
    }
    finally {
      DecompilerContext.setProperty(DecompilerContext.CURRENT_METHOD_WRAPPER, outerWrapper);
    }
  }

  private static String toValidJavaIdentifier(String name) {
    if (name == null || name.isEmpty()) return name;

    boolean changed = false;
    StringBuilder res = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if ((i == 0 && !Character.isJavaIdentifierStart(c))
          || (i > 0 && !Character.isJavaIdentifierPart(c))) {
        changed = true;
        res.append("_");
      }
      else {
        res.append(c);
      }
    }
    if (!changed) {
      return name;
    }
    return res.append("/*").append(name).append("*/").toString();
  }

  private static void recordComponentToJava(StructRecordComponent cd, TextBuffer buffer, boolean varArgComponent) {
    Map.Entry<VarType, GenericFieldDescriptor> fieldTypeData = getFieldTypeData(cd);
    VarType fieldType = fieldTypeData.getKey();
    GenericFieldDescriptor descriptor = fieldTypeData.getValue();

    appendAnnotations(buffer, -1, cd);

    final List<TypeAnnotation> typeAnnotations = TypeAnnotation.listFrom(cd);
    if (descriptor != null) {
      buffer.append(GenericMain.getGenericCastTypeName(
        varArgComponent ? descriptor.type.decreaseArrayDim() : descriptor.type,
        TypeAnnotationWriteHelper.create(typeAnnotations)
      ));
    }
    else {
      buffer.append(ExprProcessor.getCastTypeName(
        varArgComponent ? fieldType.decreaseArrayDim() : fieldType,
        TypeAnnotationWriteHelper.create(typeAnnotations)
      ));
    }
    if (varArgComponent) {
      buffer.append("...");
    }
    buffer.append(' ');

    buffer.append(cd.getName());
  }

  private static boolean hideConstructor(
    ClassesProcessor.ClassNode node,
    boolean hasAnnotation,
    boolean init,
    boolean throwsExceptions,
    int paramCount,
    int methodAccessFlags
  ) {
    if (!init || hasAnnotation|| throwsExceptions || paramCount > 0 || !DecompilerContext.getOption(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR)) {
      return false;
    }

    StructClass cl = node.getWrapper().getClassStruct();

	  int classAccessFlags = node.type == ClassesProcessor.ClassNode.CLASS_ROOT ? cl.getAccessFlags() : node.access;
    boolean isEnum = cl.hasModifier(CodeConstants.ACC_ENUM) && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_ENUM);

    // default constructor requires same accessibility flags. Exception: enum constructor which is always private
    if (!isEnum && ((classAccessFlags & ACCESSIBILITY_FLAGS) != (methodAccessFlags & ACCESSIBILITY_FLAGS))) {
  	  return false;
  	}

    int count = 0;
    for (StructMethod mt : cl.getMethods()) {
      if (CodeConstants.INIT_NAME.equals(mt.getName()) && ++count > 1) {
        return false;
      }
    }

    return true;
  }

  private static Map.Entry<VarType, GenericFieldDescriptor> getFieldTypeData(StructField fd) {
    VarType fieldType = new VarType(fd.getDescriptor(), false);

    GenericFieldDescriptor descriptor = null;
    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute attr = fd.getAttribute(StructGeneralAttribute.ATTRIBUTE_SIGNATURE);
      if (attr != null) {
        descriptor = GenericMain.parseFieldSignature(attr.getSignature());
      }
    }

    return new AbstractMap.SimpleImmutableEntry<>(fieldType, descriptor);
  }

  private static void appendDeprecation(TextBuffer buffer, int indent) {
    TextBuffer commentBuffer = new TextBuffer();
    commentBuffer.appendIndent(indent).append("/** @deprecated */").appendLineSeparator();
    buffer.append(OutputMemberSerializer.tag(commentBuffer.toString(), CommentOutputMember::new));
  }

  private enum MType {CLASS, FIELD, METHOD}

  private static void appendRenameComment(TextBuffer bufferDirect, String oldName, MType type, int indent) {
    if (oldName == null) return;
    TextBuffer buffer = new TextBuffer();

    buffer.appendIndent(indent);
    buffer.append("// $FF: renamed from: ");

    switch (type) {
      case CLASS -> buffer.append(ExprProcessor.buildJavaClassName(oldName));
      case FIELD -> {
        String[] fParts = oldName.split(" ");
        FieldDescriptor fd = FieldDescriptor.parseDescriptor(fParts[2]);
        buffer.append(fParts[1]);
        buffer.append(' ');
        buffer.append(getTypePrintOut(fd.type));
      }
      default -> {
        String[] mParts = oldName.split(" ");
        MethodDescriptor md = MethodDescriptor.parseDescriptor(mParts[2]);
        buffer.append(mParts[1]);
        buffer.append(" (");
        boolean first = true;
        for (VarType paramType : md.params) {
          if (!first) {
            buffer.append(", ");
          }
          first = false;
          buffer.append(getTypePrintOut(paramType));
        }
        buffer.append(") ");
        buffer.append(getTypePrintOut(md.ret));
      }
    }

    buffer.appendLineSeparator();
    bufferDirect.append(OutputMemberSerializer.tag(buffer.toString(), CommentOutputMember::new));
  }

  private static String getTypePrintOut(VarType type) {
    String typeText = ExprProcessor.getCastTypeName(type, false, Collections.emptyList());
    if (ExprProcessor.UNDEFINED_TYPE_STRING.equals(typeText) &&
        DecompilerContext.getOption(IFernflowerPreferences.UNDEFINED_PARAM_TYPE_OBJECT)) {
      typeText = ExprProcessor.getCastTypeName(VarType.VARTYPE_OBJECT, false, Collections.emptyList());
    }
    return typeText;
  }

  private static void appendComment(TextBuffer buffer, String comment, int indent) {
    TextBuffer commentBuffer = new TextBuffer();
    commentBuffer.appendIndent(indent).append("// ").append(comment).appendLineSeparator();
    buffer.append(OutputMemberSerializer.tag(commentBuffer.toString(), CommentOutputMember::new));
  }

  private static void appendAnnotations(TextBuffer buffer, int indent, StructMember mb) {
    for (StructGeneralAttribute.Key<?> key : StructGeneralAttribute.ANNOTATION_ATTRIBUTES) {
      StructAnnotationAttribute attribute = (StructAnnotationAttribute)mb.getAttribute(key);
      if (attribute != null) {
        for (AnnotationExprent annotation : attribute.getAnnotations()) {
          if (mb.memberAnnCollidesWithTypeAnnotation(annotation)) continue;
          String text = annotation.toJava(indent, BytecodeMappingTracer.DUMMY).toString();
          buffer.append(text);
          if (indent < 0) {
            buffer.append(' ');
          }
          else {
            buffer.appendLineSeparator();
          }
        }
      }
    }
  }

  private static void appendParameterAnnotations(TextBuffer buffer, StructMethod mt, Type type, int param) {
    for (StructGeneralAttribute.Key<?> key : StructGeneralAttribute.PARAMETER_ANNOTATION_ATTRIBUTES) {
      StructAnnotationParameterAttribute attribute = (StructAnnotationParameterAttribute)mt.getAttribute(key);
      if (attribute != null) {
        List<List<AnnotationExprent>> annotations = attribute.getParamAnnotations();
        if (param < annotations.size()) {
          for (AnnotationExprent annotation : annotations.get(param)) {
            if (mt.paramAnnCollidesWithTypeAnnotation(annotation, type, param)) continue;
            String text = annotation.toJava(-1, BytecodeMappingTracer.DUMMY).toString();
            buffer.append(text).append(' ');
          }
        }
      }
    }
  }

  private static final Map<Integer, String> MODIFIERS;
  static {
    MODIFIERS = new LinkedHashMap<>();
    MODIFIERS.put(CodeConstants.ACC_PUBLIC, "public");
    MODIFIERS.put(CodeConstants.ACC_PROTECTED, "protected");
    MODIFIERS.put(CodeConstants.ACC_PRIVATE, "private");
    MODIFIERS.put(CodeConstants.ACC_ABSTRACT, "abstract");
    MODIFIERS.put(CodeConstants.ACC_STATIC, "static");
    MODIFIERS.put(CodeConstants.ACC_FINAL, "final");
    MODIFIERS.put(CodeConstants.ACC_STRICT, "strictfp");
    MODIFIERS.put(CodeConstants.ACC_TRANSIENT, "transient");
    MODIFIERS.put(CodeConstants.ACC_VOLATILE, "volatile");
    MODIFIERS.put(CodeConstants.ACC_SYNCHRONIZED, "synchronized");
    MODIFIERS.put(CodeConstants.ACC_NATIVE, "native");
  }

  private static final int CLASS_ALLOWED =
    CodeConstants.ACC_PUBLIC | CodeConstants.ACC_PROTECTED | CodeConstants.ACC_PRIVATE | CodeConstants.ACC_ABSTRACT |
    CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL | CodeConstants.ACC_STRICT;
  private static final int FIELD_ALLOWED =
    CodeConstants.ACC_PUBLIC | CodeConstants.ACC_PROTECTED | CodeConstants.ACC_PRIVATE | CodeConstants.ACC_STATIC |
    CodeConstants.ACC_FINAL | CodeConstants.ACC_TRANSIENT | CodeConstants.ACC_VOLATILE;
  private static final int METHOD_ALLOWED =
    CodeConstants.ACC_PUBLIC | CodeConstants.ACC_PROTECTED | CodeConstants.ACC_PRIVATE | CodeConstants.ACC_ABSTRACT |
    CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL | CodeConstants.ACC_SYNCHRONIZED | CodeConstants.ACC_NATIVE |
    CodeConstants.ACC_STRICT;

  private static final int CLASS_EXCLUDED = CodeConstants.ACC_ABSTRACT | CodeConstants.ACC_STATIC;
  private static final int FIELD_EXCLUDED = CodeConstants.ACC_PUBLIC | CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL;
  private static final int METHOD_EXCLUDED = CodeConstants.ACC_PUBLIC | CodeConstants.ACC_ABSTRACT;

  private static final int ACCESSIBILITY_FLAGS = CodeConstants.ACC_PUBLIC | CodeConstants.ACC_PROTECTED | CodeConstants.ACC_PRIVATE;

  private static void appendModifiers(TextBuffer outputBuffer, int flags, int allowed, boolean isInterface, int excluded) {
    TextBuffer buffer = new TextBuffer();
    flags &= allowed;
    if (!isInterface) excluded = 0;
    for (int modifier : MODIFIERS.keySet()) {
      if ((flags & modifier) == modifier && (modifier & excluded) == 0) {
        buffer.append(MODIFIERS.get(modifier)).append(' ');
      }
    }
    outputBuffer.append(OutputMemberSerializer.keyword(buffer.toString()));
  }

  public static GenericClassDescriptor getGenericClassDescriptor(StructClass cl) {
    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute attr = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_SIGNATURE);
      if (attr != null) {
        return GenericMain.parseClassSignature(attr.getSignature());
      }
    }
    return null;
  }

  public static void appendTypeParameters(
    TextBuffer buffer,
    List<String> parameters,
    List<? extends List<GenericType>> bounds,
    final List<TypeAnnotation> typeAnnotations
  ) {
    buffer.append('<');
    for (int i = 0; i < parameters.size(); i++) {
      if (i > 0) {
        buffer.append(", ");
      }
      TargetInfo.TypeParameterTarget.extract(typeAnnotations, i).forEach(typeAnnotation -> typeAnnotation.writeTo(buffer));
      buffer.append(parameters.get(i));
      List<GenericType> parameterBounds = bounds.get(i);
      if (parameterBounds.size() > 1 || !"java/lang/Object".equals(parameterBounds.get(0).getValue())) {
        buffer.append(" extends ");
        TargetInfo.TypeParameterBoundTarget.extract(typeAnnotations, i, 0).forEach(typeAnnotation -> typeAnnotation.writeTo(buffer));
        buffer.append(GenericMain.getGenericCastTypeName(parameterBounds.get(0), Collections.emptyList()));
        for (int j = 1; j < parameterBounds.size(); j++) {
          buffer.append(" & ");
          TargetInfo.TypeParameterBoundTarget.extract(typeAnnotations, i, j).forEach(typeAnnotation -> typeAnnotation.writeTo(buffer));
          buffer.append(GenericMain.getGenericCastTypeName(parameterBounds.get(j), Collections.emptyList()));
        }
      }
    }

    buffer.append('>');
  }

  private static void appendFQClassNames(TextBuffer buffer, List<String> names) {
    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      buffer.appendIndent(2).append(name);
      if (i < names.size() - 1) {
        buffer.append(',').appendLineSeparator();
      }
    }
  }
}