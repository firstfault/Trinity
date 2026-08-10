package me.f1nal.trinity.gui.windows.impl.assembler;

import imgui.flag.ImGuiDir;
import imgui.internal.ImGuiDockNode;
import imgui.type.ImInt;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.gui.DisplayManager;
import me.f1nal.trinity.gui.windows.WindowManager;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerWindow;

/** Creates and reuses the default right-side dock shared by assembler windows. */
final class AssemblerDocking {
    private static final float ASSEMBLER_WIDTH_RATIO = 0.40F;
    private static int assemblerDockNodeId;

    private AssemblerDocking() {
    }

    static int resolveDefaultDock(AssemblerFrame assembler) {
        WindowManager windows = Main.getWindowManager();
        DecompilerWindow decompiler = findDecompilerAnchor(windows,
                assembler.getMethodInput().getOwningClass());
        int existingAssemblerNode = windows.getWindowsOfType(AssemblerFrame.class).stream()
                .filter(window -> window != assembler && window.isVisible())
                .mapToInt(AssemblerFrame::getCurrentDockId)
                .filter(AssemblerDocking::isValidDockNode)
                .findFirst().orElse(0);
        if (existingAssemblerNode != 0) {
            assemblerDockNodeId = existingAssemblerNode;
            return existingAssemblerNode;
        }
        if (isValidDockNode(assemblerDockNodeId)) {
            return assemblerDockNodeId;
        }

        int decompilerDockNode = decompiler == null ? 0 : decompiler.getCurrentDockId();
        if (!isValidDockNode(decompilerDockNode)) {
            ImGuiDockNode centralNode = imgui.internal.ImGui.dockBuilderGetCentralNode(
                    DisplayManager.getMainDockspaceId());
            decompilerDockNode = centralNode != null && centralNode.isValidPtr()
                    ? centralNode.getID() : 0;
        }
        if (!isValidDockNode(decompilerDockNode)) return 0;

        ImInt assemblerNode = new ImInt();
        ImInt decompilerNode = new ImInt();
        imgui.internal.ImGui.dockBuilderSplitNode(decompilerDockNode, ImGuiDir.Right,
                ASSEMBLER_WIDTH_RATIO, assemblerNode, decompilerNode);
        assemblerDockNodeId = assemblerNode.get();
        if (!isValidDockNode(assemblerDockNodeId)) return 0;

        imgui.internal.ImGui.dockBuilderFinish(DisplayManager.getMainDockspaceId());
        return assemblerDockNodeId;
    }

    private static DecompilerWindow findDecompilerAnchor(WindowManager windows, ClassInput owner) {
        DecompilerWindow focused = windows.getFocusedWindow(DecompilerWindow.class);
        if (focused != null && isValidDockNode(focused.getCurrentDockId())) return focused;

        DecompilerWindow matching = windows.getWindowsOfType(DecompilerWindow.class).stream()
                .filter(DecompilerWindow::isVisible)
                .filter(window -> window.getSelectedClass() == owner)
                .filter(window -> isValidDockNode(window.getCurrentDockId()))
                .findFirst().orElse(null);
        if (matching != null) return matching;

        return windows.getWindowsOfType(DecompilerWindow.class).stream()
                .filter(DecompilerWindow::isVisible)
                .filter(window -> isValidDockNode(window.getCurrentDockId()))
                .findFirst().orElse(null);
    }

    private static boolean isValidDockNode(int nodeId) {
        if (nodeId == 0) return false;
        ImGuiDockNode node = imgui.internal.ImGui.dockBuilderGetNode(nodeId);
        return node != null && node.isValidPtr();
    }
}
