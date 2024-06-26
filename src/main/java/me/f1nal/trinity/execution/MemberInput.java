package me.f1nal.trinity.execution;

import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.remap.DisplayName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class MemberInput<N> extends Input<N> {
    private final ClassInput owner;
    private final MemberDetails details;
    private final DisplayName displayName;

    protected MemberInput(N node, ClassInput owner, MemberDetails details) {
        super(node);
        this.owner = owner;
        this.details = details;
        this.displayName = new DisplayName(details.getName());
    }

    @Override
    public final ClassInput getOwningClass() {
        return owner;
    }

    @Override
    public Map<String, Function<Input<?>, String>> getCopyableElements() {
        return COPYABLE_ELEMENTS;
    }

    public final MemberDetails getDetails() {
        return details;
    }

    @Override
    public final DisplayName getDisplayName() {
        return displayName;
    }

    public final String getDescriptor() {
        return details.getDesc();
    }

    @Override
    public XrefBuilder createXrefBuilder(XrefMap xrefMap) {
        return new XrefBuilderMemberRef(xrefMap, this.getDetails());
    }

    @Override
    public void populatePopup(PopupItemBuilder builder) {
        super.populatePopup(builder);
    }

    @Override
    public String toString() {
        return getDetails().getKey();
    }

    private static final Map<String, Function<Input<?>, String>> COPYABLE_ELEMENTS = new LinkedHashMap<>() {{
            put("Owner", input -> ((MemberInput<?>)input).getDetails().getOwner());
            put("Name", input -> ((MemberInput<?>)input).getDetails().getName());
            put("Descriptor", input -> ((MemberInput<?>)input).getDetails().getDesc());
    }};
}
