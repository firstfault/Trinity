// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.main.decompiler;

import me.f1nal.trinity.decompiler.main.Fernflower;
import org.jetbrains.annotations.Nullable;
import me.f1nal.trinity.decompiler.main.CancellationManager;
import me.f1nal.trinity.decompiler.main.extern.IBytecodeProvider;
import me.f1nal.trinity.decompiler.main.extern.IFernflowerLogger;
import me.f1nal.trinity.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.Map;

@SuppressWarnings("unused")
public class BaseDecompiler {
  private final Fernflower engine;

  public BaseDecompiler(IBytecodeProvider provider, IResultSaver saver, @Nullable Map<String, Object> options, IFernflowerLogger logger) {
    this(provider, saver, options, logger, null);
  }

  public BaseDecompiler(IBytecodeProvider provider, IResultSaver saver, @Nullable Map<String, Object> options, IFernflowerLogger logger,
                        @Nullable CancellationManager cancellationManager) {
    engine = new Fernflower(provider, saver, options, logger, cancellationManager);
  }

  public void addSource(File source) {
    engine.addSource(source);
  }

  public void addLibrary(File library) {
    engine.addLibrary(library);
  }

  public void decompileContext() {
    try {
      engine.decompileContext();
    }
    finally {
      engine.clearContext();
    }
  }
}