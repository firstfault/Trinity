package me.f1nal.trinity.gui.viewport.dnd;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.windows.impl.project.create.NewProjectFrame;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DragAndDropHandler implements GLFWDropCallbackI {
    private final List<File> droppedFiles = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void invoke(long hnd, int count, long names) {
        for (int i = 0; i < count; i++) {
            String path = GLFWDropCallback.getName(names, i);
            File file = new File(path);

            this.droppedFiles.add(file);
        }
    }

    public void draw() {
        if (this.droppedFiles.isEmpty()) {
            return;
        }
        this.handleDroppedFiles();
        this.droppedFiles.clear();

/*        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImVec2 mousePos = ImGui.getMousePos();
        if (mousePos.x >= mainViewport.getWorkPosX() && mousePos.x <= mainViewport.getWorkPosX() + mainViewport.getWorkSizeX() &&
                mousePos.y >= mainViewport.getWorkPosY() && mousePos.y <= mainViewport.getWorkPosY() + mainViewport.getWorkSizeY()) {

        }*/
    }

    private void handleDroppedFiles() {
        for (File file : this.droppedFiles) {
            if (isDatabaseFile(file)) {
                Main.getDisplayManager().openDatabase(file.getAbsolutePath());
                return;
            }
        }

        NewProjectFrame newProject = Main.getWindowManager().addStaticWindow(NewProjectFrame.class);
        for (File file : this.droppedFiles) {
            newProject.getInputTab().getFileListComponent().addFile(file);
        }
    }

    private static boolean isDatabaseFile(File file) {
        boolean database = false;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);

            if ('T' == dataInputStream.readChar()) {
                database = true;
            }

            dataInputStream.close();
        } catch (IOException e) {
            return false;
        }
        return database;
    }
}
