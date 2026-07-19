package me.f1nal.trinity.gui.windows.impl;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.appdata.PreferencesFile;
import me.f1nal.trinity.decompiler.output.FontEnum;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.gui.components.general.EnumComboBox;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.gui.windows.impl.xref.SearchMaxDisplay;
import me.f1nal.trinity.keybindings.Bindable;
import me.f1nal.trinity.keybindings.KeyBindManager;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeManager;
import me.f1nal.trinity.util.GuiUtil;

public class PreferencesFrame extends StaticWindow {
    private final String id = ComponentId.getId(this.getClass());
    private final EnumComboBox<NumberDisplayTypeEnum> numberDisplayTypeComboBox;
    private final EnumComboBox<SearchMaxDisplay> searchMaxDisplayComboBox;
    private final PreferencesFile preferencesFile;
    private Bindable editingBind;
    private boolean captureArmed;
    private String keyMappingStatus;

    public PreferencesFrame(Trinity trinity) {
        super("Preferences", 450, 250, trinity);
        this.preferencesFile = Main.getPreferences();
        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoDocking;
        this.numberDisplayTypeComboBox = new EnumComboBox<>("Number Display Type", NumberDisplayTypeEnum.values(), preferencesFile.getDefaultNumberDisplayType());
        this.searchMaxDisplayComboBox = new EnumComboBox<>("Search Element Limit", SearchMaxDisplay.values(), preferencesFile.getSearchMaxDisplay());
    }

    @Override
    protected void renderFrame() {
        if (ImGui.beginTabBar("preferences tab" + id)) {
            if (ImGui.beginTabItem("General")) {
                ThemeManager themeManager = Main.getThemeManager();
                if (ImGui.beginCombo("Theme", themeManager.getCurrentTheme().getName())) {
                    for (Theme theme : themeManager.getThemes()) {
                        if (ImGui.selectable(theme.getName(), themeManager.getCurrentTheme() == theme)) {
                            themeManager.setCurrentTheme(theme);
                        }
                    }
                    ImGui.endCombo();
                }

                this.preferencesFile.getDefaultFont().drawControls();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Interactivity")) {
                if (ImGui.checkbox("Auto-Follow Xref", preferencesFile.isAutoviewXref())) {
                    preferencesFile.setAutoviewXref(!preferencesFile.isAutoviewXref());
                }
                this.tooltip("If a cross reference gets viewed and it contains only one entry, automatically follow it in the decompiler.");

                this.preferencesFile.setSearchMaxDisplay(searchMaxDisplayComboBox.draw());
                this.tooltip("Limits the amount of cross references displayed in the Xref Viewer window.");
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Decompiler")) {
                this.preferencesFile.getDecompilerFont().drawControls();
                this.preferencesFile.setDefaultNumberDisplayType(this.numberDisplayTypeComboBox.draw());

                if (ImGui.checkbox("Hide comments", preferencesFile.isDecompilerHideComments())) {
                    preferencesFile.setDecompilerHideComments(!preferencesFile.isDecompilerHideComments());
                }

                if (ImGui.checkbox("Normalize text", preferencesFile.isDecompilerNormalizeText())) {
                    preferencesFile.setDecompilerNormalizeText(!preferencesFile.isDecompilerNormalizeText());
                }

                if (ImGui.checkbox("Treat enum as class", preferencesFile.isDecompilerEnumClass())) {
                    preferencesFile.setDecompilerEnumClass(!preferencesFile.isDecompilerEnumClass());
                }
                GuiUtil.tooltip("Treat enums as plain Java classes. Requires decompiler refresh.");

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Key Mappings")) {
                this.drawKeyMappings();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    private void tooltip(String tooltip) {
        if (ImGui.isItemHovered()) ImGui.setTooltip(tooltip);
    }

    private void drawKeyMappings() {
        KeyBindManager manager = Main.getKeyBindManager();
        ImGui.textDisabled("Multiple key combinations supported. Escape cancels capture.");

        for (String category : manager.getCategories()) {
            ImGui.separator();
            ImGui.text(category);
            for (Bindable bindable : manager.getBindables()) {
                if (!bindable.getCategory().equals(category)) continue;
                ImGui.text(bindable.getDisplayName());
                ImGui.sameLine(210.F);
                String mapping = this.editingBind == bindable ? "Press shortcut..." : bindable.getKeyName();
                if (ImGui.button(mapping + "###mapping." + bindable.getIdentifier(), 145.F, 0.F)) {
                    this.editingBind = bindable;
                    this.captureArmed = false;
                    this.keyMappingStatus = null;
                }
                ImGui.sameLine();
                if (ImGui.smallButton("Clear###clear." + bindable.getIdentifier())) {
                    this.persistBinding(manager.bind(bindable, -1, false, false, false, false), bindable);
                    if (this.editingBind == bindable) this.editingBind = null;
                }
                ImGui.sameLine();
                if (ImGui.smallButton("Reset###reset." + bindable.getIdentifier())) {
                    Bindable conflict = manager.reset(bindable);
                    if (conflict != null) preferencesFile.setKeyBinding(conflict.createData());
                    preferencesFile.removeKeyBinding(bindable.getIdentifier());
                    this.keyMappingStatus = conflict == null ? null
                            : "Cleared conflicting mapping from " + conflict.getDisplayName() + ".";
                    if (this.editingBind == bindable) this.editingBind = null;
                }
            }
        }

        if (keyMappingStatus != null) ImGui.textDisabled(keyMappingStatus);
        this.captureKeyMapping();
    }

    private void captureKeyMapping() {
        if (this.editingBind == null) return;
        if (!this.captureArmed) {
            this.captureArmed = true;
            return;
        }
        if (ImGui.isKeyPressed(ImGuiKey.Escape, false)) {
            this.editingBind = null;
            return;
        }
        for (int key = ImGuiKey.NamedKey_BEGIN; key <= ImGuiKey.Oem102; key++) {
            if (Bindable.isModifierKey(key) || !ImGui.isKeyPressed(key, false)) continue;
            Bindable changed = this.editingBind;
            Bindable conflict = Main.getKeyBindManager().bind(changed, key,
                    ImGui.getIO().getKeyCtrl(), ImGui.getIO().getKeyShift(),
                    ImGui.getIO().getKeyAlt(), ImGui.getIO().getKeySuper());
            this.persistBinding(conflict, changed);
            this.editingBind = null;
            return;
        }
    }

    private void persistBinding(Bindable conflict, Bindable changed) {
        preferencesFile.setKeyBinding(changed.createData());
        if (conflict != null) {
            preferencesFile.setKeyBinding(conflict.createData());
            this.keyMappingStatus = "Cleared conflicting mapping from " + conflict.getDisplayName() + ".";
        } else {
            this.keyMappingStatus = null;
        }
    }
}
