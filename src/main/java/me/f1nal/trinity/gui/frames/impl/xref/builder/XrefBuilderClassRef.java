package me.f1nal.trinity.gui.frames.impl.xref.builder;

import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Xref builder for class references.
 */
public class XrefBuilderClassRef extends XrefBuilder {
    private final String className;

    public XrefBuilderClassRef(XrefMap xrefMap, String className) {
        super(xrefMap);
        this.className = className;
    }

    @Override
    public String getTitle() {
        return this.className;
    }

    @Override
    public Collection<AbstractXref> createXrefs() {
        return new ArrayList<>(getXrefMap().getReferences(this.className));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XrefBuilderClassRef that = (XrefBuilderClassRef) o;
        return Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }
}
