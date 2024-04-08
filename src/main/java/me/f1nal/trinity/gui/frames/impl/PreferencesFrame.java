package me.f1nal.trinity.gui.frames.impl;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.flag.ImGuiSliderFlags;
import imgui.type.ImFloat;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.appdata.PreferencesFile;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.gui.components.general.EnumComboBox;
import me.f1nal.trinity.gui.frames.ClosableWindow;
import me.f1nal.trinity.gui.frames.impl.xref.SearchMaxDisplay;
import me.f1nal.trinity.keybindings.Bindable;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeManager;
import me.f1nal.trinity.util.GuiUtil;

public class PreferencesFrame extends ClosableWindow {
    private final String id = ComponentId.getId(this.getClass());
    private final EnumComboBox<NumberDisplayTypeEnum> numberDisplayTypeComboBox;
    private final EnumComboBox<SearchMaxDisplay> searchMaxDisplayComboBox;
    private final PreferencesFile preferencesFile;
    private Bindable editingBind;

    public PreferencesFrame(Trinity trinity) {
        super("Preferences", 500, 310, trinity);
        this.preferencesFile = Main.getPreferences();
        this.numberDisplayTypeComboBox = new EnumComboBox<>("Default Number Display Type", NumberDisplayTypeEnum.values(), preferencesFile.getDefaultNumberDisplayType());
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

                ImFloat fontSize = new ImFloat(Main.getPreferences().getFontSize());
                ImGui.inputScalar("Font Size", ImGuiDataType.Float, fontSize, 0.5F, 1.F, "%.2f px", ImGuiSliderFlags.AlwaysClamp);
                Main.getPreferences().setFontSize(fontSize.get());

                if (Main.getDisplayManager().getFontManager().getCurrentFontSize() != fontSize.get()) {
                    ImGui.textDisabled("Please restart Trinity for a clear version of the font.");
                }

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
                this.preferencesFile.setDefaultNumberDisplayType(this.numberDisplayTypeComboBox.draw());

                if (ImGui.checkbox("Hide comments", preferencesFile.isDecompilerHideComments())) {
                    preferencesFile.setDecompilerHideComments(!preferencesFile.isDecompilerHideComments());
                    Main.getDisplayManager().getArchiveEntryViewerFacade().resetDecompilerComponents();
                }

                if (ImGui.checkbox("Normalize text", preferencesFile.isDecompilerNormalizeText())) {
                    preferencesFile.setDecompilerNormalizeText(!preferencesFile.isDecompilerNormalizeText());
                    Main.getDisplayManager().getArchiveEntryViewerFacade().resetDecompilerComponents();
                }

                if (ImGui.checkbox("Treat enum as class", preferencesFile.isDecompilerEnumClass())) {
                    preferencesFile.setDecompilerEnumClass(!preferencesFile.isDecompilerEnumClass());
                }
                GuiUtil.tooltip("Treat enums as plain Java classes. Requires decompiler refresh.");

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Keybindings")) {
                for (Bindable bindable : Main.getKeyBindManager().getBindables()) {
                    ImGui.text(bindable.getDisplayName());
                    ImGui.sameLine();
                    if (ImGui.smallButton(this.editingBind == bindable ? "Press A Key..." : bindable.getKeyName())) {
                        this.editingBind = bindable;
                    }

                    if (this.editingBind == bindable) {
                        for (int i = 0; i < 512; i++) {
                            if (ImGui.isKeyDown(i)) {
                                this.editingBind.bind(i);
                                this.editingBind = null;
                                break;
                            }
                        }
                    }
                }
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    private void tooltip(String tooltip) {
        if (ImGui.isItemHovered()) ImGui.setTooltip(tooltip);
    }
}
