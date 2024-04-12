package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.access.AccessFlagsMaskProvider;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.IRenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.IXrefBuilderProvider;
import me.f1nal.trinity.remap.DisplayName;
import me.f1nal.trinity.remap.IDisplayNameProvider;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.remap.RenameType;

public abstract class Input<N> implements AccessFlagsMaskProvider, IRenameHandler, IXrefBuilderProvider, IDisplayNameProvider {
    private final N node;
    private final AccessFlags accessFlags;

    protected Input(N node) {
        this.node = node;
        this.accessFlags = new AccessFlags(this);
    }

    public final N getNode() {
        return node;
    }

    public final AccessFlags getAccessFlags() {
        return accessFlags;
    }

    public abstract InputType getType();
    public abstract ClassInput getOwningClass();
    public abstract boolean isAccessFlagValid(AccessFlags.Flag flag);
    public abstract void rename(Remapper remapper, String newName);
    public void populatePopup(PopupItemBuilder builder) {
        Trinity trinity = getOwningClass().getExecution().getTrinity();

        addXrefViewerMenuItem(trinity, builder);
        builder.menuItem("View Member", () -> {
            Main.getDisplayManager().openDecompilerView(this);
        });

        DisplayName displayName = getDisplayName();
        builder.disabled(() -> displayName.getType() == RenameType.NONE, (items) -> {
            items.menuItem("Revert Name", () -> {
                displayName.setName(displayName.getOriginalName());
                Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
            });
        });
    }
}
