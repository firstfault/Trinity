// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.main.rels;

import me.f1nal.trinity.decompiler.code.CodeConstants;
import me.f1nal.trinity.decompiler.main.CancellationManager;
import me.f1nal.trinity.decompiler.main.DecompilerContext;
import me.f1nal.trinity.decompiler.main.collectors.CounterContainer;
import me.f1nal.trinity.decompiler.main.collectors.VarNamesCollector;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerLogger;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerPreferences;
import me.f1nal.trinity.decompiler.main.extern.IDecompilationProgressListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ClassWrapper {
  private final StructClass classStruct;
  private final Set<String> hiddenMembers = ConcurrentHashMap.newKeySet();
  private final VBStyleCollection<Exprent, String> staticFieldInitializers = new VBStyleCollection<>();
  private final VBStyleCollection<Exprent, String> dynamicFieldInitializers = new VBStyleCollection<>();
  private final VBStyleCollection<MethodWrapper, String> methods = new VBStyleCollection<>();

  public ClassWrapper(StructClass classStruct) {
    this.classStruct = classStruct;
  }

  public void init() {
    init(method -> { });
  }

  public void init(Consumer<StructMethod> methodDecompiled) {
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS, classStruct);
    DecompilerContext.setProperty(DecompilerContext.CURRENT_CLASS_WRAPPER, this);
    DecompilerContext.getLogger().startClass(classStruct.qualifiedName);

    boolean testMode = DecompilerContext.getOption(IFernflowerPreferences.UNIT_TEST_MODE);
    DecompilerContext parentContext = DecompilerContext.getCurrentContext();
    DecompilerContext.MethodContextFactory methodContextFactory =
      parentContext.createMethodContextFactory();
    List<StructMethod> sourceMethods = new ArrayList<>(classStruct.getMethods());
    int threadCount = testMode ? 1 : getMethodThreadCount(sourceMethods.size());
    IDecompilationProgressListener progressListener =
      DecompilerContext.getDecompilationProgressListener();

    try {
      if (testMode) {
        for (StructMethod method : sourceMethods) {
          installMethod(processMethod(method, methodContextFactory, true), methodDecompiled);
        }
      }
      else if (threadCount <= 1) {
        processPrioritizedSequentially(sourceMethods, methodContextFactory,
          progressListener, methodDecompiled);
      }
      else {
        processPrioritizedInParallel(sourceMethods, methodContextFactory, progressListener,
          methodDecompiled, threadCount);
      }
    }
    finally {
      DecompilerContext.setCurrentContext(parentContext);
      DecompilerContext.getLogger().endClass();
    }
  }

  private void processPrioritizedSequentially(
    List<StructMethod> sourceMethods,
    DecompilerContext.MethodContextFactory methodContextFactory,
    IDecompilationProgressListener progressListener,
    Consumer<StructMethod> methodDecompiled
  ) {
    MethodScheduler scheduler = new MethodScheduler(
      classStruct.qualifiedName, sourceMethods, progressListener);
    MethodResult[] orderedResults = new MethodResult[sourceMethods.size()];
    int nextInstall = 0;
    ScheduledMethod scheduled;
    while ((scheduled = scheduler.claimNext()) != null) {
      MethodResult result = processMethod(scheduled.method(), methodContextFactory, false);
      orderedResults[scheduled.index()] = result;
      markMethodProcessed(result);
      while (nextInstall < orderedResults.length && orderedResults[nextInstall] != null) {
        installMethod(orderedResults[nextInstall++], methodDecompiled);
      }
    }
  }

  private void processPrioritizedInParallel(
    List<StructMethod> sourceMethods,
    DecompilerContext.MethodContextFactory methodContextFactory,
    IDecompilationProgressListener progressListener,
    Consumer<StructMethod> methodDecompiled,
    int threadCount
  ) {
    // Initialize lazy class state once before workers begin reading it.
    classStruct.getPool();
    MethodScheduler scheduler = new MethodScheduler(
      classStruct.qualifiedName, sourceMethods, progressListener);
    BlockingQueue<CompletedMethod> completions = new LinkedBlockingQueue<>();
    AtomicBoolean stopped = new AtomicBoolean();
    AtomicInteger threadNumber = new AtomicInteger();
    ExecutorService executor = Executors.newFixedThreadPool(threadCount, runnable -> {
      Thread thread = new Thread(runnable,
        "Fernflower Method " + threadNumber.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    });

    try {
      // Keep only one long-lived task per worker. A worker claims its next method only after it
      // finishes the current one, so a viewport change never has to reprioritize a 10,000-item
      // executor queue.
      for (int worker = 0; worker < threadCount; worker++) {
        executor.execute(() -> runMethodWorker(
          scheduler, methodContextFactory, completions, stopped));
      }

      MethodResult[] orderedResults = new MethodResult[sourceMethods.size()];
      int nextInstall = 0;
      for (int completed = 0; completed < sourceMethods.size(); completed++) {
        CompletedMethod completion = takeCompletion(completions);
        if (completion.failure() != null) {
          throwUnchecked(completion.failure());
        }
        MethodResult result = completion.result();
        orderedResults[completion.index()] = result;
        markMethodProcessed(result);
        // Fernflower's class-wide writers assume the installed wrapper collection is always a
        // classfile-order prefix. Analysis may finish in any order, but only the contiguous
        // completed prefix is committed and progressively rendered.
        while (nextInstall < orderedResults.length && orderedResults[nextInstall] != null) {
          installMethod(orderedResults[nextInstall++], methodDecompiled);
        }
      }
    }
    finally {
      stopped.set(true);
      executor.shutdownNow();
    }
  }

  private void runMethodWorker(
    MethodScheduler scheduler,
    DecompilerContext.MethodContextFactory methodContextFactory,
    BlockingQueue<CompletedMethod> completions,
    AtomicBoolean stopped
  ) {
    while (!stopped.get()) {
      ScheduledMethod scheduled = scheduler.claimNext();
      if (scheduled == null) return;
      try {
        MethodResult result = processMethod(scheduled.method(), methodContextFactory, false);
        completions.add(new CompletedMethod(scheduled.index(), result, null));
      }
      catch (Throwable throwable) {
        if (stopped.compareAndSet(false, true)) {
          completions.add(new CompletedMethod(scheduled.index(), null, throwable));
        }
        return;
      }
    }
  }

  private MethodResult processMethod(StructMethod mt,
                                     DecompilerContext.MethodContextFactory methodContextFactory,
                                     boolean testMode) {
    DecompilerContext previousContext = DecompilerContext.getCurrentContext();
    DecompilerContext methodContext = methodContextFactory.create();
    DecompilerContext.setCurrentContext(methodContext);
    DecompilerContext.getLogger().startMethod(mt.getName() + " " + mt.getDescriptor());

    try {
      MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());
      VarProcessor varProc = new VarProcessor(classStruct, mt, md);
      DecompilerContext.startMethod(varProc);

      VarNamesCollector vc = varProc.getVarNamesCollector();
      CounterContainer counter = DecompilerContext.getCounterContainer();
      CancellationManager cancellationManager = DecompilerContext.getCancellationManager();
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
                new MethodProcessorRunnable(classStruct, mt, md, varProc, context);
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

      if (!isError) {
        // rename vars so that no one has the same name as a field
        VarNamesCollector namesCollector = new VarNamesCollector();
        classStruct.getFields().forEach(f -> namesCollector.addName(f.getName()));
        varProc.refreshVarNames(namesCollector);

        applyParameterNames(mt, md, varProc);
        applyDebugInfo(mt, varProc, methodWrapper);
      }
      return new MethodResult(mt, methodWrapper);
    }
    finally {
      DecompilerContext.getLogger().endMethod();
      DecompilerContext.setCurrentContext(previousContext);
    }
  }

  private void installMethod(MethodResult result, Consumer<StructMethod> methodDecompiled) {
    StructMethod method = result.method();
    MethodWrapper wrapper = result.wrapper();
    methods.addWithKey(wrapper, InterpreterUtil.makeUniqueKey(method.getName(), method.getDescriptor()));
    DecompilerContext.restoreMethod(wrapper.varproc, wrapper.counter);
    methodDecompiled.accept(method);
  }

  private void markMethodProcessed(MethodResult result) {
    StructMethod method = result.method();
    DecompilerContext.getDecompilationProgressListener().methodProcessed(
      classStruct.qualifiedName, method.getName(), method.getDescriptor());
  }

  private static CompletedMethod takeCompletion(BlockingQueue<CompletedMethod> completions) {
    try {
      return completions.take();
    }
    catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new CancellationManager.CanceledException(exception);
    }
  }

  private static void throwUnchecked(Throwable cause) {
    if (cause instanceof CancellationManager.CanceledException canceledException) {
      throw canceledException;
    }
    if (cause instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    if (cause instanceof Error error) {
      throw error;
    }
    throw new RuntimeException(cause);
  }

  private static int getMethodThreadCount(int methodCount) {
    Object value = DecompilerContext.getProperty(IFernflowerPreferences.METHOD_PROCESSING_THREADS);
    int configured;
    try {
      configured = Integer.parseInt(String.valueOf(value));
    }
    catch (NumberFormatException ignored) {
      configured = 1;
    }
    return Math.max(1, Math.min(configured, methodCount));
  }

  private record MethodResult(StructMethod method, MethodWrapper wrapper) { }

  private record ScheduledMethod(int index, StructMethod method) { }

  private record CompletedMethod(int index, MethodResult result, Throwable failure) { }

  /**
   * Claims visible methods first and otherwise advances a single classfile-order cursor. Claiming
   * costs O(visible methods), not O(all methods), after the one-time index construction.
   */
  private static final class MethodScheduler {
    private final String owner;
    private final List<StructMethod> methods;
    private final IDecompilationProgressListener progressListener;
    private final Map<IDecompilationProgressListener.MethodKey, Integer> methodIndexes;
    private final boolean[] claimed;
    private int nextIndexed;

    private MethodScheduler(String owner, List<StructMethod> methods,
                            IDecompilationProgressListener progressListener) {
      this.owner = owner;
      this.methods = methods;
      this.progressListener = progressListener;
      this.claimed = new boolean[methods.size()];
      this.methodIndexes = new HashMap<>(Math.max(16, methods.size()));
      for (int index = 0; index < methods.size(); index++) {
        StructMethod method = methods.get(index);
        methodIndexes.put(new IDecompilationProgressListener.MethodKey(
          method.getName(), method.getDescriptor()), index);
      }
    }

    private synchronized ScheduledMethod claimNext() {
      List<IDecompilationProgressListener.MethodKey> priorities =
        progressListener.priorityMethods(owner);
      for (int priorityIndex = 0; priorityIndex < priorities.size(); priorityIndex++) {
        IDecompilationProgressListener.MethodKey key = priorities.get(priorityIndex);
        Integer index = methodIndexes.get(key);
        if (index != null && !claimed[index]) {
          claimed[index] = true;
          return new ScheduledMethod(index, methods.get(index));
        }
      }

      while (nextIndexed < methods.size() && claimed[nextIndexed]) {
        nextIndexed++;
      }
      if (nextIndexed >= methods.size()) return null;
      int index = nextIndexed++;
      claimed[index] = true;
      return new ScheduledMethod(index, methods.get(index));
    }
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
