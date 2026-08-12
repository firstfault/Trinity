package me.f1nal.trinity.gui.windows.impl.xref.builder;

import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.events.EventIdentityRefactored;

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

    /** Updates a stored query identity after an atomic bytecode rename. */
    public void onIdentityRefactored(EventIdentityRefactored event) {
    }
}
