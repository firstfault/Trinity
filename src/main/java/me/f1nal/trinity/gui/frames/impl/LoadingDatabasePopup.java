package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.gui.frames.Popup;
import me.f1nal.trinity.gui.frames.impl.project.create.NewProjectFrame;

import java.io.File;

public class LoadingDatabasePopup extends Popup {
    private final File path;

    public LoadingDatabasePopup(Trinity trinity, File path) {
        super("Loading Database...", trinity);
        this.path = path;
        closeOnEscape = false;
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Please wait while the last program database is being loaded.");
        if (DatabaseLoader.load.get(this.path).isDone()) {
            boolean status = DatabaseLoader.load.getStatus();
            close();
            if (!status) {
                Main.getDisplayManager().addStaticWindow(NewProjectFrame.class);
            }
            DatabaseLoader.load.clear();
            Main.getAppDataManager().getState().setDatabaseLoaded(true);
        }
    }
}
