package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.util.SearchTermMatchable;

public abstract class AbstractXref implements SearchTermMatchable, IKind {
    private final XrefWhere where;
    private final XrefKind kind;

    protected AbstractXref(XrefWhere where, XrefKind kind) {
        this.where = where;
        this.kind = kind;
    }

    public abstract XrefAccessType getAccess();
    public abstract String getInvocation();
    public final XrefWhere getWhere() {
        return where;
    }
    @Override
    public final XrefKind getKind() {
        return kind;
    }

    @Override
    public boolean matches(String searchTerm) {
        String lowerCase = searchTerm.toLowerCase();
        return getWhere().getText().toLowerCase().contains(lowerCase) || getInvocation().toLowerCase().contains(lowerCase);
    }
}
