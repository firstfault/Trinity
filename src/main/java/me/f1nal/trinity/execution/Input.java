package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.access.AccessFlagsMaskProvider;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.cp.IRenameHandler;
import me.f1nal.trinity.gui.windows.impl.xref.builder.IXrefBuilderProvider;
import me.f1nal.trinity.remap.Remapper;

public abstract class Input implements AccessFlagsMaskProvider, IRenameHandler, IXrefBuilderProvider {
    public abstract ClassInput getOwningClass();
    public abstract AccessFlags getAccessFlags();
    public abstract boolean isAccessFlagValid(AccessFlags.Flag flag);
    public abstract void rename(Remapper remapper, String newName);
    public void populatePopup(PopupItemBuilder builder) {
        Trinity trinity = getOwningClass().getExecution().getTrinity();

        addXrefViewerMenuItem(trinity, builder);
        builder.menuItem("View Member", () -> {
            Main.getDisplayManager().openDecompilerView(this);
        });
    }

    public abstract String getDisplayName();
}
