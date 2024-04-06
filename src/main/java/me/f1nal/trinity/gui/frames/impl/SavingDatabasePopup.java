package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.gui.frames.Popup;

import java.util.function.Consumer;

public class SavingDatabasePopup extends Popup {
    private final Consumer<Boolean> callback;

    public SavingDatabasePopup(Trinity trinity, Consumer<Boolean> callback) {
        super("Saving Database...", trinity);
        this.callback = callback;
        closeOnEscape = false;
    }

    @Override
    protected void renderFrame() {
        ImGui.text("Please wait while the program database is being saved.\nExiting forcefully may result in corruption.");
//        ImGui.newLine();
//        ImGui.textDisabled("Serializing Objects (1/32)");
//        ImGui.progressBar(0.3F);
        if (DatabaseLoader.save.get(trinity.getDatabase().getPath()).isDone()) {
            close();
            callback.accept(DatabaseLoader.save.getStatus());
        }
    }
}
