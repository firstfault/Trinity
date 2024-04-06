package me.f1nal.trinity.execution;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.access.AccessFlagsMaskProvider;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.frames.impl.xref.XrefViewerFrame;
import me.f1nal.trinity.gui.frames.impl.xref.builder.XrefBuilder;
import me.f1nal.trinity.remap.Remapper;

public abstract class Input implements AccessFlagsMaskProvider {
    public abstract ClassInput getOwningClass();
    public abstract AccessFlags getAccessFlags();
    public abstract boolean isAccessFlagValid(AccessFlags.Flag flag);
    public abstract void rename(Remapper remapper, String newName);
    public void populatePopup(PopupItemBuilder builder) {
        Trinity trinity = getOwningClass().getExecution().getTrinity();

        builder.menuItem("View Xrefs", () -> {
            Main.getDisplayManager().addClosableWindow(new XrefViewerFrame(this.createXrefBuilder(trinity.getExecution().getXrefMap()), trinity));
        });
        builder.menuItem("View Member", () -> {
            Main.getDisplayManager().openDecompilerView(this);
        });
    }
    protected abstract XrefBuilder createXrefBuilder(XrefMap xrefMap);
}
