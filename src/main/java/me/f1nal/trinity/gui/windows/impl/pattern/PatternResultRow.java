package me.f1nal.trinity.gui.windows.impl.pattern;

import me.f1nal.trinity.execution.pattern.InstructionPatternMatch;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.gui.components.general.table.IWhere;
import me.f1nal.trinity.util.SearchTermMatchable;

import java.util.Locale;

public final class PatternResultRow implements SearchTermMatchable, IKind, IWhere {
    private final InstructionPatternMatch match;
    private final PatternMatchWhere where;
    private final String summary;
    private final String searchText;

    public PatternResultRow(InstructionPatternMatch match, String patternSummary) {
        this.match = match;
        this.where = new PatternMatchWhere(match, patternSummary);
        String first = match.formattedInstructions().lines().findFirst().orElse("instruction");
        this.summary = match.instructions().size() == 1
                ? first : first + "  +" + (match.instructions().size() - 1);
        this.searchText = match.formattedInstructions() + " " + where.getText() + " "
                + match.method().getDescriptor();
    }

    public String getSummary() {
        return summary;
    }

    public InstructionPatternMatch getMatch() {
        return match;
    }

    @Override
    public XrefWhere getWhere() {
        return where;
    }

    @Override
    public XrefKind getKind() {
        return XrefKind.INVOKE;
    }

    @Override
    public boolean matches(String searchTerm) {
        return searchText.contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        return searchText.toLowerCase(Locale.ROOT).contains(searchTerm.toLowerCase(Locale.ROOT));
    }
}
