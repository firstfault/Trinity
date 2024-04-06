package me.f1nal.trinity.gui.frames.impl.assembler.history;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import me.f1nal.trinity.gui.frames.ClosableWindow;
import me.f1nal.trinity.gui.frames.impl.assembler.AssemblerFrame;

public class AssemblerHistoryFrame extends ClosableWindow {
    private final AssemblerFrame assemblerFrame;

    public AssemblerHistoryFrame(AssemblerFrame assemblerFrame) {
        super("Assembler History", 520, 150, assemblerFrame.getTrinity());
        this.assemblerFrame = assemblerFrame;
    }

    @Override
    public void render() {
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, ImGui.getStyle().getWindowPaddingY() + 4.F);
        super.render();
        ImGui.popStyleVar();
    }

    @Override
    protected void renderFrame() {
        ImGui.pushStyleVar(ImGuiStyleVar.CellPadding, 0.F, 4.F);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 8.F);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0);
        ImVec2 extraPadding = ImGui.getStyle().getTouchExtraPadding();
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, 4.F);

        AssemblerHistory[] list = assemblerFrame.getHistory().getStack().toArray(AssemblerHistory[]::new);

        for (int i = list.length - 1; i >= 0; i--) {
            AssemblerHistory history = list[i];

            ImGui.treeNodeEx(this.assemblerFrame.getId("###HistorySelectable"), ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.SpanFullWidth | ImGuiTreeNodeFlags.NoTreePushOnOpen);
            ImGui.sameLine();
            history.getBrowserViewerNode().setSuffix(history.getText());
            history.getBrowserViewerNode().draw();
        }

        ImGui.popStyleColor(2);
        ImGui.getStyle().setTouchExtraPadding(extraPadding.x, extraPadding.y);
        ImGui.popStyleVar(2);
    }
}
