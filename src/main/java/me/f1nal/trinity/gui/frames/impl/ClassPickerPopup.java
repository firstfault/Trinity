package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.gui.frames.Popup;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ClassPickerPopup extends Popup {
    private final SearchBar searchBar = new SearchBar();
    private final Predicate<ClassTarget> valid;
    private final Consumer<ClassTarget> consumer;

    public ClassPickerPopup(Trinity trinity, Predicate<ClassTarget> valid, Consumer<ClassTarget> consumer) {
        super("Class Picker", trinity);
        this.valid = valid;
        this.consumer = consumer;
    }

    @Override
    protected void renderFrame() {
        String search = searchBar.drawAndGet();
        ClassTarget input = null;
        ImGui.beginChild("class search child" + getPopupId(), 430, 200);
        if (!ImGui.beginTable("class search table" + getPopupId(), 1, ImGuiTableFlags.Borders | ImGuiTableFlags.SizingFixedFit | ImGuiTableFlags.Sortable)) {
            return;
        }
        ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.NoResize);
        ImGui.tableHeadersRow();
        for (Map.Entry<String, ClassTarget> className : trinity.getExecution().getClassTargetMap().entrySet()) {
            if (!valid.test(className.getValue())) {
                continue;
            }

            String name = className.getValue().getDisplayOrRealName();
            if (name.contains(search)) {
                if (input == null) input = className.getValue();
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.text(name);
                if (ImGui.isItemHovered()) ImGui.setTooltip("Click to select");
                if (ImGui.isItemClicked(0)) {
                    searchBar.getSearchText().set(name);
                }
            }
        }
        ImGui.endTable();
        ImGui.endChild();
        if (input == null) ImGui.beginDisabled();
        else ImGui.text(input.getDisplayOrRealName());
        if (ImGui.smallButton("Select this class")) {
            this.closeWithClass(input);
        }
        if (input == null) ImGui.endDisabled();
    }

    public void closeWithClass(ClassTarget classInput) {
        this.consumer.accept(classInput);
        this.close();
    }
}
