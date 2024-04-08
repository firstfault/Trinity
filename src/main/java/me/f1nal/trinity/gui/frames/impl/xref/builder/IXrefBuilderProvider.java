package me.f1nal.trinity.gui.frames.impl.xref.builder;

import me.f1nal.trinity.execution.xref.XrefMap;

public interface IXrefBuilderProvider {
    XrefBuilder createXrefBuilder(XrefMap xrefMap);
}
