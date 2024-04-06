package me.f1nal.trinity.gui.frames.impl.xref.builder;

import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefMap;

import java.util.Collection;

public abstract class XrefBuilder {
    private final XrefMap xrefMap;

    protected XrefBuilder(XrefMap xrefMap) {
        this.xrefMap = xrefMap;
    }

    public XrefMap getXrefMap() {
        return xrefMap;
    }

    public abstract String getTitle();
    public abstract Collection<AbstractXref> createXrefs();
}
