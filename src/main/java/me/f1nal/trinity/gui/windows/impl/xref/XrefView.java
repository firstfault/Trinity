package me.f1nal.trinity.gui.windows.impl.xref;

import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.util.SearchTermMatchable;

public class XrefView implements SearchTermMatchable {
    private final AbstractXref xref;
    private final String access;
    private final String invocation;
    private final String where;

    public XrefView(AbstractXref xref, String access, String invocation, String where) {
        this.xref = xref;
        this.access = access;
        this.invocation = invocation;
        this.where = where;
    }

    public AbstractXref getXref() {
        return xref;
    }

    public String getAccess() {
        return access;
    }

    public String getInvocation() {
        return invocation;
    }

    public String getWhere() {
        return where;
    }

    @Override
    public boolean matches(String searchTerm) {
        return invocation.toLowerCase().contains(searchTerm.toLowerCase());
    }
}
