package me.f1nal.trinity.gui.windows.impl.constant;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.gui.components.general.EnumComboBox;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchType;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeNull;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeNumber;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeString;

import java.util.ArrayList;
import java.util.List;

public class ConstantSearchFrame extends StaticWindow {
    private final EnumComboBox<ConstantSearchType> searchTypeCombo;
    private static final MemorableCheckboxComponent closeFrame = new MemorableCheckboxComponent("closeFrameAfterConstantSearch", "Close After Search", false);

    public ConstantSearchFrame(Trinity trinity) {
        super("Constant Search", 100, 100, trinity);
        this.windowFlags = ImGuiWindowFlags.AlwaysAutoResize;
        this.searchTypeCombo = new EnumComboBox<>("Constant Type", new ConstantSearchType[]{
                new ConstantSearchTypeString(trinity),
                new ConstantSearchTypeNumber.ConstantSearchTypeDecimal(trinity),
                new ConstantSearchTypeNumber.ConstantSearchTypeInteger(trinity),
                new ConstantSearchTypeNumber.ConstantSearchTypeLong(trinity),
                new ConstantSearchTypeNumber.ConstantSearchTypeFloat(trinity),
                new ConstantSearchTypeNumber.ConstantSearchTypeDouble(trinity),
                new ConstantSearchTypeNull(trinity),
        });
    }

    @Override
    protected void renderFrame() {
        ConstantSearchType type = searchTypeCombo.draw();
        boolean result = type.draw();
        if (!result) ImGui.beginDisabled();
        if (ImGui.button("Search")) {
            List<ConstantViewCache> constantList = new ArrayList<>();
            type.populate(constantList);
            Main.getWindowManager().addClosableWindow(new ConstantViewFrame(trinity, constantList));
            if (closeFrame.isChecked()) this.close();
        }
        if (!result) ImGui.endDisabled();
        ImGui.sameLine();
        closeFrame.draw();
    }
}
