package me.f1nal.trinity.gui.frames.impl.project.settings;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.gui.components.tabs.TabFrame;

public abstract class AbstractProjectSettingsTab implements TabFrame {
    private final Trinity trinity;

    protected AbstractProjectSettingsTab(Trinity trinity) {
        this.trinity = trinity;
    }

    public final Trinity getTrinity() {
        return trinity;
    }

    public final Database getDatabase() {
        return trinity.getDatabase();
    }
}
