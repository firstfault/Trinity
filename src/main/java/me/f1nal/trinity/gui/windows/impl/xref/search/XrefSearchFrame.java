package me.f1nal.trinity.gui.windows.impl.xref.search;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.components.general.EnumComboBox;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.xref.XrefViewerFrame;

public class XrefSearchFrame extends StaticWindow {
    private final EnumComboBox<XrefSearchType> xrefTypeCombo;
    private static final MemorableCheckboxComponent closeFrame = new MemorableCheckboxComponent("closeFrameAfterXrefSearch", false);

    public XrefSearchFrame(Trinity trinity) {
        super("Search Xrefs", 400, -1, trinity);
        XrefSearchTypeClass xrefSearchTypeClass = new XrefSearchTypeClass(trinity);
        XrefSearchType[] xrefTypes = new XrefSearchType[]{xrefSearchTypeClass, new XrefSearchTypeMember(trinity, xrefSearchTypeClass.getClassSelectComponent())};
        this.windowFlags = ImGuiWindowFlags.AlwaysAutoResize;
        this.xrefTypeCombo = new EnumComboBox<>("Xref Type", xrefTypes);
    }

    @Override
    protected void renderFrame() {
        XrefSearchType type = xrefTypeCombo.draw();
        boolean result = type.draw();
        if (!result) ImGui.beginDisabled();
        if (ImGui.button("Search")) {
            Main.getWindowManager().addClosableWindow(new XrefViewerFrame(type.search(), trinity, false));
            if (closeFrame.getState()) this.close();
        }
        if (!result) ImGui.endDisabled();
        ImGui.sameLine();
        closeFrame.drawCheckbox("Close After Search");
    }
}
