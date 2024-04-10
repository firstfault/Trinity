// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.main;

import me.f1nal.trinity.decompiler.main.extern.*;
import me.f1nal.trinity.decompiler.struct.lazy.LazyLoader;
import org.jetbrains.annotations.Nullable;
import me.f1nal.trinity.decompiler.main.extern.*;
import me.f1nal.trinity.decompiler.modules.renamer.ConverterHelper;
import me.f1nal.trinity.decompiler.modules.renamer.IdentifierConverter;
import me.f1nal.trinity.decompiler.modules.renamer.PoolInterceptor;
import me.f1nal.trinity.decompiler.struct.IDecompiledData;
import me.f1nal.trinity.decompiler.struct.StructClass;
import me.f1nal.trinity.decompiler.struct.StructContext;
import me.f1nal.trinity.decompiler.util.TextBuffer;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Fernflower implements IDecompiledData {
  private final StructContext structContext;
  private final ClassesProcessor classProcessor;
  private final IIdentifierRenamer helper;
  private final IdentifierConverter converter;

  public Fernflower(IBytecodeProvider provider,
                    IResultSaver saver,
                    @Nullable Map<String, Object> customProperties,
                    IFernflowerLogger logger,
                    @Nullable CancellationManager cancellationManager) {
    Map<String, Object> properties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
    if (customProperties != null) {
      properties.putAll(customProperties);
    }

    String level = (String)properties.get(IFernflowerPreferences.LOG_LEVEL);
    if (level != null) {
      try {
        logger.setSeverity(IFernflowerLogger.Severity.valueOf(level.toUpperCase(Locale.ENGLISH)));
      }
      catch (IllegalArgumentException ignore) { }
    }

    structContext = new StructContext(saver, this, new LazyLoader(provider));
    classProcessor = new ClassesProcessor(structContext);

    PoolInterceptor interceptor = null;
    if ("1".equals(properties.get(IFernflowerPreferences.RENAME_ENTITIES))) {
      helper = loadHelper((String)properties.get(IFernflowerPreferences.USER_RENAMER_CLASS), logger);
      interceptor = new PoolInterceptor();
      converter = new IdentifierConverter(structContext, helper, interceptor);
    }
    else {
      helper = null;
      converter = null;
    }

    DecompilerContext context = new DecompilerContext(properties, logger, structContext, classProcessor, interceptor, cancellationManager);
    DecompilerContext.setCurrentContext(context);
  }

  public Fernflower(IBytecodeProvider provider, IResultSaver saver, Map<String, Object> customProperties, IFernflowerLogger logger) {
    this(provider, saver, customProperties, logger, null);
  }

  private static IIdentifierRenamer loadHelper(String className, IFernflowerLogger logger) {
    if (className != null) {
      try {
        Class<?> renamerClass = Fernflower.class.getClassLoader().loadClass(className);
        return (IIdentifierRenamer) renamerClass.getDeclaredConstructor().newInstance();
      }
      catch (Exception e) {
        logger.writeMessage("Cannot load renamer '" + className + "'", IFernflowerLogger.Severity.WARN, e);
      }
    }

    return new ConverterHelper();
  }

  public void addSource(File source) {
    structContext.addSpace(source, true);
  }

  public void addLibrary(File library) {
    structContext.addSpace(library, false);
  }

  public void decompileContext() {
    if (converter != null) {
      converter.rename();
    }

    classProcessor.loadClasses(helper);

    structContext.saveContext();
  }

  public void clearContext() {
    DecompilerContext.setCurrentContext(null);
  }

  @Override
  public String getClassEntryName(StructClass cl, String entryName) {
    ClassesProcessor.ClassNode node = classProcessor.getMapRootClasses().get(cl.qualifiedName);
    if (node.type != ClassesProcessor.ClassNode.CLASS_ROOT) {
      return null;
    }
    else if (converter != null) {
      String simpleClassName = cl.qualifiedName.substring(cl.qualifiedName.lastIndexOf('/') + 1);
      return entryName.substring(0, entryName.lastIndexOf('/') + 1) + simpleClassName + ".java";
    }
    else {
      return entryName.substring(0, entryName.lastIndexOf(".class")) + ".java";
    }
  }

  @Override
  public String getClassContent(StructClass cl) {
    try {
      TextBuffer buffer = new TextBuffer(ClassesProcessor.AVERAGE_CLASS_SIZE);
      buffer.append(DecompilerContext.getProperty(IFernflowerPreferences.BANNER).toString());
      classProcessor.writeClass(cl, buffer);
      return buffer.toString();
    }
    catch (CancellationManager.CanceledException e) {
      e.printStackTrace();
      throw e;
    }
    catch (Throwable t) {
      t.printStackTrace();
      DecompilerContext.getLogger().writeMessage("Class " + cl.qualifiedName + " couldn't be fully decompiled.", t);
      return null;
    }
  }
}