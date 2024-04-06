// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.sforms;

import me.f1nal.trinity.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.Exprent;
import me.f1nal.trinity.decompiler.modules.decompiler.stats.BasicBlockStatement;

import java.util.ArrayList;
import java.util.List;


public class DirectNode {
  public final @NotNull DirectNodeType type;
  public final @NotNull String id;
  public final @NotNull Statement statement;
  public final @Nullable BasicBlockStatement block;
  public final List<DirectNode> successors = new ArrayList<>();
  public final List<DirectNode> predecessors = new ArrayList<>();
  public List<Exprent> exprents = new ArrayList<>();

  public DirectNode(@NotNull DirectNodeType type, @NotNull Statement statement, @NotNull String id) {
    this.type = type;
    this.statement = statement;
    this.id = id;
    this.block = null;
  }

  public DirectNode(@NotNull DirectNodeType type, @NotNull Statement statement, @NotNull BasicBlockStatement block) {
    this.type = type;
    this.statement = statement;
    this.id = Integer.toString(block.id);
    this.block = block;
  }

  @Override
  public String toString() {
    return id;
  }

  public enum DirectNodeType {
    DIRECT,
    TAIL,
    INIT,
    CONDITION,
    INCREMENT,
    TRY
  }
}
