package me.f1nal.trinity.gui.viewport.dnd;

import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DragAndDropHandler implements GLFWDropCallbackI {
    private final List<File> droppedFiles = new ArrayList<>();

    @Override
    public void invoke(long hnd, int count, long names) {
        synchronized (this.droppedFiles) {
            for (int i = 0; i < count; i++) {
                String path = GLFWDropCallback.getName(names, i);
                File file = new File(path);

                this.droppedFiles.add(file);
            }
        }
    }

    public void draw() {
        File[] droppedFiles;

        synchronized (this.droppedFiles) {
            if (this.droppedFiles.isEmpty()) {
                return;
            }
            droppedFiles = this.droppedFiles.toArray(File[]::new);
            this.droppedFiles.clear();
        }

        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImVec2 mousePos = ImGui.getMousePos();
        if (mousePos.x >= mainViewport.getWorkPosX() && mousePos.x <= mainViewport.getWorkPosX() + mainViewport.getWorkSizeX() &&
                mousePos.y >= mainViewport.getWorkPosY() && mousePos.y <= mainViewport.getWorkPosY() + mainViewport.getWorkSizeY()) {
            System.out.println("DEROPPED! " + mousePos.toString());
        }
    }
}
