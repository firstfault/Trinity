package me.f1nal.trinity.gui.frames.impl.xref.builder;

import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Xref builder for class member references (e.g. fields and methods).
 */
public class XrefBuilderMemberRef extends XrefBuilder {
    private final MemberDetails details;

    public XrefBuilderMemberRef(XrefMap xrefMap, MemberDetails details) {
        super(xrefMap);
        this.details = details;
    }

    @Override
    public String getTitle() {
        return details.getAll();
    }

    @Override
    public Collection<AbstractXref> createXrefs() {
        return new ArrayList<>(getXrefMap().getReferences(details));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XrefBuilderMemberRef that = (XrefBuilderMemberRef) o;
        return Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(details);
    }
}
