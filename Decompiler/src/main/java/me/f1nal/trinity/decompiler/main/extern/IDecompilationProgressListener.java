package me.f1nal.trinity.decompiler.main.extern;

/**
 * Receives source fragments while a class is being written.
 */
public interface IDecompilationProgressListener {
  IDecompilationProgressListener NONE = (owner, name, descriptor, content) -> { };

  void methodDecompiled(String owner, String name, String descriptor, String content);
}
