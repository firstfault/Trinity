package me.f1nal.trinity.gui.frames.impl.annotation;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ClassSelectComponent;
import me.f1nal.trinity.gui.frames.Popup;
import me.f1nal.trinity.gui.frames.impl.ldc.CstChooserPopup;
import me.f1nal.trinity.gui.frames.impl.ldc.types.CstTypeByte;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public class AnnotationInsertPopup extends Popup {
    private final ClassSelectComponent classSelectComponent = new ClassSelectComponent(trinity, (target) -> target.getInput() != null && target.getInput().getAccessFlags().isAnnotation());
    private final List<AnnotationKeyValue> keyValueList = new ArrayList<>();

    public AnnotationInsertPopup(Trinity trinity) {
        super("Insert Annotation", trinity);
    }

    @Override
    protected void renderFrame() {
        this.classSelectComponent.draw();
        if (ImGui.smallButton("Create")) {
            keyValueList.add(new AnnotationKeyValue("newKey" + keyValueList.size()));
        }
        ImGui.sameLine();
        if (ImGui.smallButton("Clear")) {
            keyValueList.clear();
        }
        this.drawKeyValueTable();

    }

    private void drawKeyValueTable() {
        if (ImGui.beginTable("annotation keys" + getPopupId(), 2, ImGuiTableFlags.Borders)) {
            ImGui.tableSetupColumn("Key", ImGuiTableColumnFlags.WidthFixed, 70.0f);
            ImGui.tableSetupColumn("Value");
            ImGui.tableHeadersRow();

            AnnotationKeyValue delete = null;
            int index = 0;
            for (AnnotationKeyValue keyValue : keyValueList) {
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                if (keyValue.isEditing()) {
                    if (ImGui.inputText("###" + getStrId("editText" + index), keyValue.getEditField(), ImGuiInputTextFlags.EnterReturnsTrue)) {
                        keyValue.stopEditing();
                    }
                } else {
                    ImGui.text(keyValue.getKey());

                    String keyPopupId = getStrId("key" + index);

                    if (ImGui.isItemHovered()) {
                        if (ImGui.isItemClicked(1)) {
                            ImGui.openPopup(keyPopupId);
                        } else if (ImGui.isItemClicked(0)) {
                            keyValue.setEditing();
                        }
                        if (!ImGui.isPopupOpen(keyPopupId)) {
                            ImGui.setTooltip("LCLICK to edit name, RCLICK for options");
                        }
                    }

                    if (ImGui.beginPopup(keyPopupId)) {
                        if (ImGui.menuItem("Edit Key")) {
                            keyValue.setEditing();
                        }
                        if (ImGui.menuItem("Delete")) {
                            delete = keyValue;
                        }

                        ImGui.endPopup();
                    }
                }

                ImGui.tableSetColumnIndex(1);
                if (ImGui.smallButton("Edit")) {
                    Main.getDisplayManager().addPopup(new CstChooserPopup(trinity, new CstTypeByte()));
                }
                ImGui.sameLine();
                if (keyValue.getValue() == null) {
                    ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, "Not Set");
                } else {
                    ImGui.text(keyValue.getValue().toString());
                }
                ++index;
            }

            if (delete != null) {
                keyValueList.remove(delete);
            }

            ImGui.endTable();
        }
    }
}
