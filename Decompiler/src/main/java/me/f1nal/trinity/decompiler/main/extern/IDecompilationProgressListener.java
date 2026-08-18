package me.f1nal.trinity.decompiler.main.extern;

import java.util.List;

/**
 * Receives source fragments while a class is being written.
 */
public interface IDecompilationProgressListener {
  IDecompilationProgressListener NONE = (owner, name, descriptor, content) -> { };

  void methodDecompiled(String owner, String name, String descriptor, String content);

  /** Called once when method analysis completes, even if progressive source cannot be written yet. */
  default void methodProcessed(String owner, String name, String descriptor) {
  }

  /**
   * Returns methods which should be processed before the next method in classfile order. The
   * returned list must be an immutable snapshot; implementations may replace it as the viewport
   * changes.
   */
  default List<MethodKey> priorityMethods(String owner) {
    return List.of();
  }

  record MethodKey(String name, String descriptor) { }
}
