package me.f1nal.trinity.gui.windows.impl.variablexref;

import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.DecompilerVariableReference;
import me.f1nal.trinity.gui.components.filter.kind.IKind;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.util.SearchTermMatchable;

final class VariableXrefRow implements IKind, SearchTermMatchable {
    private final DecompilerVariableReference reference;
    private final VariableXrefLocation location;

    VariableXrefRow(DecompiledClass decompiledClass, DecompilerVariableReference reference) {
        this.reference = reference;
        this.location = new VariableXrefLocation(decompiledClass, reference);
    }

    DecompilerVariableReference reference() {
        return reference;
    }

    VariableXrefLocation location() {
        return location;
    }

    @Override
    public IKindType getKind() {
        return reference.access();
    }

    @Override
    public boolean matches(String searchTerm) {
        return searchableText().contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        return searchableText().toLowerCase(java.util.Locale.ROOT)
                .contains(searchTerm.toLowerCase(java.util.Locale.ROOT));
    }

    private String searchableText() {
        return reference.access().getName() + " " + location.getText() + " "
                + location.variableName() + " "
                + reference.methodInput().getDisplayName().getName();
    }
}
