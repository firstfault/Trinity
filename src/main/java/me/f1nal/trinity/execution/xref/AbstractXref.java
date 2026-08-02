package me.f1nal.trinity.execution.xref;

import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.gui.components.filter.kind.IKindTypeName;
import me.f1nal.trinity.gui.components.general.table.IWhere;
import me.f1nal.trinity.util.SearchTermMatchable;

public abstract class AbstractXref implements SearchTermMatchable, IKind, IKindTypeName, IWhere {
    private final XrefWhere where;
    private final XrefKind kind;

    protected AbstractXref(XrefWhere where, XrefKind kind) {
        this.where = where;
        this.kind = kind;
    }

    public abstract XrefAccessType getAccess();
    public abstract String getInvocation();
    @Override
    public final String getKindTypeName() {
        return getInvocation();
    }
    @Override
    public final XrefWhere getWhere() {
        return where;
    }
    @Override
    public final XrefKind getKind() {
        return kind;
    }
    public final String getAccessText() {
        return getAccess().getText();
    }

    @Override
    public boolean matches(String searchTerm) {
        String lowerCase = searchTerm.toLowerCase();
        return getWhere().getText().toLowerCase().contains(lowerCase) || getInvocation().toLowerCase().contains(lowerCase);
    }
}
