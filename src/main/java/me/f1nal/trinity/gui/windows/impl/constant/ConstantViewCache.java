package me.f1nal.trinity.gui.windows.impl.constant;

import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.util.SearchTermMatchable;

public class ConstantViewCache implements SearchTermMatchable, IKind {
    private final String constant;
    private final XrefWhere where;
    private final XrefKind kind;

    public ConstantViewCache(String constant, XrefWhere where, XrefKind kind) {
        this.constant = constant;
        this.where = where;
        this.kind = kind;
    }

    public String getConstant() {
        return constant;
    }

    public XrefWhere getWhere() {
        return where;
    }

    @Override
    public XrefKind getKind() {
        return kind;
    }

    @Override
    public boolean matches(String searchTerm) {
        return this.getWhere().getText().contains(searchTerm);
    }
}
