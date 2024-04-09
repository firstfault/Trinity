package me.f1nal.trinity.gui.windows.impl.xref.builder;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.xref.XrefMap;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;

public interface IXrefBuilderProvider {
    XrefBuilder createXrefBuilder(XrefMap xrefMap);
    default void addXrefViewerMenuItem(Trinity trinity, PopupItemBuilder builder) {
        builder.menuItem("View Xrefs", () -> {
            Main.getWindowManager().addClosableWindow(new XrefViewerFrame(this.createXrefBuilder(trinity.getExecution().getXrefMap()), trinity));
        });
    }
}
