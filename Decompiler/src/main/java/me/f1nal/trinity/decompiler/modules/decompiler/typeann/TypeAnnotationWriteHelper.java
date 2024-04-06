// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package me.f1nal.trinity.decompiler.modules.decompiler.typeann;

import me.f1nal.trinity.decompiler.util.TextBuffer;
import org.jetbrains.annotations.NotNull;
import me.f1nal.trinity.decompiler.struct.StructTypePathEntry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link TypeAnnotation} to maintain the state of the {@link StructTypePathEntry} list while writing.
 */
public class TypeAnnotationWriteHelper {
  private final @NotNull Deque<StructTypePathEntry> paths;

  private final @NotNull TypeAnnotation annotation;

  public TypeAnnotationWriteHelper(@NotNull TypeAnnotation annotation) {
    this(annotation, new ArrayDeque<>(annotation.getPaths()));
  }

  public TypeAnnotationWriteHelper(@NotNull TypeAnnotation annotation, @NotNull Deque<StructTypePathEntry> paths) {
    this.annotation = annotation;
    this.paths = paths;
  }

  /**
   * @return Active path relative to the current scope when writing.
   */
  public @NotNull Deque<StructTypePathEntry> getPaths() {
    return paths;
  }

  /**
   * @return The annotation to write
   */
  public @NotNull TypeAnnotation getAnnotation() {
    return annotation;
  }

  public void writeTo(@NotNull StringBuilder sb) {
    annotation.writeTo(sb);
  }

  public void writeTo(@NotNull TextBuffer sb) {
    annotation.writeTo(sb);
  }

  public int arrayPathCount() {
    return (int) paths.stream().filter(entry -> entry.getTypePathEntryKind() == StructTypePathEntry.Kind.ARRAY.getId()).count();
  }

  public static List<TypeAnnotationWriteHelper> create(List<TypeAnnotation> typeAnnotations) {
    return typeAnnotations.stream()
      .map(TypeAnnotationWriteHelper::new)
      .collect(Collectors.toList());
  }
}
