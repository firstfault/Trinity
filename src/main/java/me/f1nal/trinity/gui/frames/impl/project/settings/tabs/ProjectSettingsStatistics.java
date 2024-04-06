package me.f1nal.trinity.gui.frames.impl.project.settings.tabs;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.Database;
import me.f1nal.trinity.database.DatabaseLoader;
import me.f1nal.trinity.database.object.AbstractDatabaseObject;
import me.f1nal.trinity.gui.frames.impl.project.settings.AbstractProjectSettingsTab;
import me.f1nal.trinity.util.TimeUtil;

import java.util.HashMap;
import java.util.Map;

public class ProjectSettingsStatistics extends AbstractProjectSettingsTab {
    public ProjectSettingsStatistics(Trinity trinity) {
        super(trinity);
    }

    @Override
    public void drawTabContent() {
        Database database = getDatabase();

        ImGui.text("Created " + TimeUtil.getFormattedTime(database.getCreationTime()));
        ImGui.text("Last accessed " + TimeUtil.getFormattedTime(database.getLastOpenTime()));
        ImGui.text("Object Load Time " + database.getObjectsLoadTime() + "ms");
        ImGui.text("Data Pool Load Time " + database.getDataPoolLoadTime() + "ms");

        if (ImGui.treeNode("Objects###DbStatisticsObjects")) {
            Map<Class<?>, Integer> typeMap = new HashMap<>();

            for (AbstractDatabaseObject object : database.getObjects()) {
                Integer count = typeMap.computeIfAbsent(object.getClass(), c -> 0);
                typeMap.put(object.getClass(), count + 1);
            }

            typeMap.forEach((type, count) -> {
                ImGui.text(String.format("%s has %s objects", DatabaseLoader.getAlias(type), count));
            });
            ImGui.treePop();
        }
    }

    @Override
    public String getName() {
        return "Statistics";
    }
}
