package me.f1nal.trinity.gui.frames.impl.ldc;

import imgui.ImGui;
import imgui.type.ImInt;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.frames.Popup;

import java.util.ArrayList;
import java.util.List;

public class CstChooserPopup extends Popup {
    private final List<CstType<?>> cstTypes = new ArrayList<>();
    private final String[] cstTypesLabels;
    private final ImInt cstTypeComboIndex = new ImInt();

    public CstChooserPopup(Trinity trinity, CstType<?>... cstTypes) {
        super("Edit Const", trinity);
        if (cstTypes.length == 0) {
            throw new IndexOutOfBoundsException("Need cst types here");
        }
        this.cstTypes.addAll(List.of(cstTypes));
        this.cstTypesLabels = new String[this.cstTypes.size()];
        for (int i = 0; i < this.cstTypesLabels.length; i++) {
            this.cstTypesLabels[i] = this.cstTypes.get(i).getLabel();
        }
    }

    private CstType<?> getSelectedType() {
        return cstTypes.get(this.cstTypeComboIndex.get());
    }

    @Override
    protected void renderFrame() {
        ImGui.combo("Const Type", cstTypeComboIndex, cstTypesLabels);
        CstType<?> selectedType = this.getSelectedType();
        selectedType.draw();
    }
}
