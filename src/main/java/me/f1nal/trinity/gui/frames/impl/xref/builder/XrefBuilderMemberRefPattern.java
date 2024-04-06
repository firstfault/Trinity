package me.f1nal.trinity.gui.frames.impl.xref.builder;

import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.XrefMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Xref builder for class member references (e.g. fields and methods).
 */
public class XrefBuilderMemberRefPattern extends XrefBuilder {
    private final Pattern pattern;
    private final String title;

    public XrefBuilderMemberRefPattern(XrefMap xrefMap, Pattern pattern, @Nullable String title) {
        super(xrefMap);
        this.title = title == null ? pattern.pattern() : title;
        this.pattern = pattern;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public Collection<AbstractXref> createXrefs() {
        return new ArrayList<>(getXrefMap().getMemberReferencesByPattern(pattern));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XrefBuilderMemberRefPattern that = (XrefBuilderMemberRefPattern) o;
        return Objects.equals(pattern, that.pattern) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, title);
    }
}
