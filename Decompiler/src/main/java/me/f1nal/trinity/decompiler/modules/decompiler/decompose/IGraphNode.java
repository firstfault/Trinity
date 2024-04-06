// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package me.f1nal.trinity.decompiler.modules.decompiler.decompose;

import java.util.List;

public interface IGraphNode {
  List<? extends IGraphNode> getPredecessorNodes();
}
