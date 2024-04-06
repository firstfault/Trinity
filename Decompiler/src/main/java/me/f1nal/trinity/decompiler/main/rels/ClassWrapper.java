// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.main.rels;

import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.CancellationManager;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.collectors.CounterContainer;
import me.f1nal.trinity.decompiler.main.collectors.VarNamesCollector;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerLogger;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.struct.attr.StructGeneralAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructLocalVariableTableAttribute;
import me.f1nal.trinity.decompiler.struct.attr.StructMethodParametersAttribute;
import me.f1nal.trinity.decompiler.struct.gen.MethodDescriptor;
import me.f1nal.trinity.decompiler.util.InterpreterUtil;
import me.f1nal.trinity.decompiler.util.VBStyleCollection;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.decompiler.modules.decompiler.stats.RootStatement;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarProcessor;
import me.f1nal.trinity.decompiler.modules.decompiler.vars.VarVersionPair;
import me.f1nal.trinity.decompiler.struct.StructClass;
import me.f1nal.trinity.decompiler.struct.StructMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassWrapper {
  private final StructClass classStruct;
  private final Set<String> hiddenMembers = new HashSet<>();
  private final VBStyleCollection<Exprent, String> staticFieldInitializers = new VBStyleCollection<>();
  private final VBStyleCollection<Exprent, String> dynamicFieldInitializers = new VBStyleCollection<>();
  private final VBStyleCollection<MethodWrapper, String> methods = new VBStyleCollection<>();

  public ClassWrapper(StructClass classStruct) {
    this.classStruct = classStruct;
  }

  public void init() {
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS, classStruct);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_WRAPPER, this);
    DecompilerContext.getLogger().startClass(classStruct.qualifiedName);

    boolean testMode = DecompilerContext.getOption(IFernflowerPreferences.UNIT_TEST_MODE);
    CancellationManager cancellationManager = DecompilerContext.getCancellationManager();
    for (StructMethod mt : classStruct.getMethods()) {
      DecompilerContext.getLogger().startMethod(mt.getName() + " " + mt.getDescriptor());

      MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());
      VarProcessor varProc = new VarProcessor(classStruct, mt, md);
      DecompilerContext.startMethod(varProc);

      VarNamesCollector vc = varProc.getVarNamesCollector();
      CounterContainer counter = DecompilerContext.getCounterContainer();

      RootStatement root = null;

      boolean isError = false;
      Throwable errorThrowable = null;

      try {
        cancellationManager.checkCanceled();
        if (mt.containsCode()) {
          if (testMode) {
            root = MethodProcessorRunnable.codeToJava(classStruct, mt, md, varProc);
          }
          else {
            DecompilerContext context = DecompilerContext.getCurrentContext();
            try {
              cancellationManager.startMethod(classStruct.qualifiedName, mt.getName());
              MethodProcessorRunnable mtProc =
                new MethodProcessorRunnable(classStruct, mt, md, varProc, DecompilerContext.getCurrentContext());
              mtProc.run();
              cancellationManager.checkCanceled();
              root = mtProc.getResult();
            }
            finally {
              DecompilerContext.setCurrentContext(context);
              cancellationManager.finishMethod(classStruct.qualifiedName, mt.getName());
            }
          }
        }
        else {
          int varIndex = 0;
          if (!mt.hasModifier(CodeConstants.ACC_STATIC)) {
            varProc.getThisVars().put(new VarVersionPair(0, 0), classStruct.qualifiedName);
            varProc.setVarName(new VarVersionPair(0, 0), vc.getFreeName(0));
            varIndex = 1;
          }
          for (int i = 0; i < md.params.length; i++) {
            varProc.setVarName(new VarVersionPair(varIndex, 0), vc.getFreeName(varIndex));
            varIndex += md.params[i].getStackSize();
          }
        }
      }
      catch (CancellationManager.TimeExceedException e) {
        String message = "Processing time limit exceeded for method " + mt.getName() + ", execution interrupted.";
        DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.ERROR);
        isError = true;
        errorThrowable = e;
      }
      catch (CancellationManager.CanceledException e) {
        throw e;
      }
      catch (Throwable t) {
        String message = "Method " + mt.getName() + " " + mt.getDescriptor() + " couldn't be decompiled.";
        errorThrowable = t;
        DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN, t);
        isError = true;
      }

      MethodWrapper methodWrapper = new MethodWrapper(root, varProc, mt, counter);
      methodWrapper.decompiledWithErrors = isError;
      if (errorThrowable != null) {
        methodWrapper.setErrorStacktrace(errorThrowable);
      }

      methods.addWithKey(methodWrapper, InterpreterUtil.makeUniqueKey(mt.getName(), mt.getDescriptor()));

      if (!isError) {
        // rename vars so that no one has the same name as a field
        VarNamesCollector namesCollector = new VarNamesCollector();
        classStruct.getFields().forEach(f -> namesCollector.addName(f.getName()));
        varProc.refreshVarNames(namesCollector);

        applyParameterNames(mt, md, varProc);  // if parameter names are present and should be used

        applyDebugInfo(mt, varProc, methodWrapper);  // if debug information is present and should be used
      }

      DecompilerContext.getLogger().endMethod();
    }

    DecompilerContext.getLogger().endClass();
  }

  private static void applyParameterNames(StructMethod mt, MethodDescriptor md, VarProcessor varProc) {
    if (DecompilerContext.getOption(IFernflowerPreferences.USE_METHOD_PARAMETERS)) {
      StructMethodParametersAttribute attr = mt.getAttribute(StructGeneralAttribute.ATTRIBUTE_METHOD_PARAMETERS);
      if (attr != null) {
        List<StructMethodParametersAttribute.Entry> entries = attr.getEntries();
        int index = varProc.getFirstParameterVarIndex();
        for (int i = varProc.getFirstParameterPosition(); i < entries.size(); i++) {
          StructMethodParametersAttribute.Entry entry = entries.get(i);
          if (entry.myName != null) {
            varProc.setVarName(new VarVersionPair(index, 0), entry.myName);
          }
          if ((entry.myAccessFlags & CodeConstants.ACC_FINAL) != 0) {
            varProc.setParameterFinal(new VarVersionPair(index, 0));
          }
          index += md.params[i].getStackSize();
        }
      }
    }
  }

  private static void applyDebugInfo(StructMethod mt, VarProcessor varProc, MethodWrapper methodWrapper) {
    if (DecompilerContext.getOption(IFernflowerPreferences.USE_DEBUG_VAR_NAMES)) {
      StructLocalVariableTableAttribute attr = mt.getLocalVariableAttr();
      if (attr != null) {
        // only param names here
        varProc.setDebugVarNames(attr.getMapParamNames());

        // the rest is here
        methodWrapper.getOrBuildGraph().iterateExprents(exprent -> {
          List<Exprent> lst = exprent.getAllExprents(true);
          lst.add(exprent);
          lst.stream()
            .filter(e -> e.type == Exprent.EXPRENT_VAR)
            .forEach(e -> {
              VarExprent varExprent = (VarExprent)e;
              String name = varExprent.getDebugName(mt);
              if (name != null) {
                varProc.setVarName(varExprent.getVarVersionPair(), name);
              }
            });
          return 0;
        });
      }
    }
  }

  public MethodWrapper getMethodWrapper(String name, String descriptor) {
    return methods.getWithKey(InterpreterUtil.makeUniqueKey(name, descriptor));
  }

  public StructClass getClassStruct() {
    return classStruct;
  }

  public VBStyleCollection<MethodWrapper, String> getMethods() {
    return methods;
  }

  public Set<String> getHiddenMembers() {
    return hiddenMembers;
  }

  public VBStyleCollection<Exprent, String> getStaticFieldInitializers() {
    return staticFieldInitializers;
  }

  public VBStyleCollection<Exprent, String> getDynamicFieldInitializers() {
    return dynamicFieldInitializers;
  }

  @Override
  public String toString() {
    return classStruct.qualifiedName;
  }
}