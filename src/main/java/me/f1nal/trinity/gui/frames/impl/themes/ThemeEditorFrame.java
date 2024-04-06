package me.f1nal.trinity.gui.frames.impl.themes;

import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.appdata.AppDataManager;
import me.f1nal.trinity.gui.components.general.FileSelectorComponent;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenu;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.theme.*;
import me.f1nal.trinity.util.GuiUtil;

import java.awt.*;
import java.io.File;
import java.util.List;

public class ThemeEditorFrame extends StaticWindow {
    private final ThemeManager themeManager;
    private final me.f1nal.trinity.gui.components.popup.PopupMenu popupMenu = new PopupMenu();
    private boolean setOpen;
    private Theme theme;
    private ModifiedThemeState modifiedTheme;

    public ThemeEditorFrame(Trinity trinity) {
        super("Theme Editor", 0, 0, trinity);
        this.windowFlags |= ImGuiWindowFlags.AlwaysAutoResize;
        this.windowFlags |= ImGuiWindowFlags.MenuBar;
        this.themeManager = Main.getThemeManager();
        this.theme = themeManager.getCurrentTheme();
    }

    @Override
    protected void renderFrame() {
        this.drawMenuBar();
        if (ImGui.beginListBox("###ThemeManagerThemeSelector", 0.F, 300.F)) {
            for (Theme theme : this.themeManager.getThemes()) {
                if (ImGui.selectable(theme.getName(), this.theme == theme) && (this.modifiedTheme == null || this.isModifyingTheme(theme))) this.theme = theme;
                if (ImGui.isItemClicked(1)) this.openPopup(theme);
                if (this.themeManager.getCurrentTheme() == theme) {
                    ImGui.sameLine(0.F, 0.F);
                    ImGui.textDisabled(" (Selected)");
                }
            }
            ImGui.endListBox();
        }
        ImGui.sameLine();
        if (ImGui.beginChild("ThemeManagerChild", 300, 300)) {
            this.drawThemeEditor();
        }
        ImGui.endChild();
        this.popupMenu.draw();
    }

    private void openPopup(Theme theme) {
        PopupItemBuilder popup = PopupItemBuilder.create();
        popup.menuItem("Export...", () -> {
            AppDataManager appDataManager = Main.getAppDataManager();
            FileSelectorComponent selector = new FileSelectorComponent("ExportTheme", appDataManager.getThemeFile(theme).getAbsolutePath(), (dir, name) -> name.toLowerCase().endsWith(".theme"), FileDialog.SAVE);
            File file = selector.openFileChooser();
            if (file != null) {
                appDataManager.saveTheme(theme, file);
            }
        });
        if (theme.isEditable()) {
            popup.separator();
            popup.menuItem("Delete", () -> this.themeManager.deleteThemePermanently(theme));
        }
        this.popupMenu.show(popup);
    }

    private void drawMenuBar() {
        ImGui.beginMenuBar();
        if (ImGui.beginMenu("Themes")) {
            if (ImGui.menuItem("Import...")) {
                AppDataManager appDataManager = Main.getAppDataManager();
                FileSelectorComponent selector = new FileSelectorComponent("ImportTheme", appDataManager.getThemesDirectory().getAbsolutePath(), (dir, name) -> name.toLowerCase().endsWith(".theme"), FileDialog.LOAD);
                File file = selector.openFileChooser();
                if (file != null) {
                    Theme importedTheme = appDataManager.loadTheme(appDataManager.getThemeName(file.getName()), file);
                    if (importedTheme != null) appDataManager.saveTheme(importedTheme);
                }
            }

            if (ImGui.menuItem("Refresh")) {
                Main.getAppDataManager().reloadThemes();
            }

            boolean disabled = this.modifiedTheme != null;
            if (disabled) ImGui.beginDisabled();
            if (ImGui.menuItem("Create New Theme")) {
                Main.getDisplayManager().addPopup(new ThemeNamePopup(trinity, (name) -> {
                    Theme newTheme = new Theme(name, true);
                    if (this.themeManager.addTheme(newTheme)) {
                        this.theme = newTheme;
                        this.modifiedTheme = new ModifiedThemeState(theme);
                    }
                }));
            }
            if (disabled) ImGui.endDisabled();

            ImGui.endMenu();
        }
        ImGui.endMenuBar();
    }

    @Override
    public void close() {
        if (this.modifiedTheme != null) {
            return;
        }

        super.close();
    }

    private void drawThemeEditor() {
        boolean setOpen = this.setOpen;
        if (!setOpen) this.setOpen = true;

        if (GuiUtil.smallCheckbox("Selected", themeManager.getCurrentTheme() == theme)) {
            themeManager.setCurrentTheme(theme);
        }

        if (!this.theme.isEditable()) {
            ImGui.sameLine();
            ImGui.textDisabled("Default Theme (?)");
            GuiUtil.tooltip("Default Themes cannot be edited.");
        } else if (this.isModifyingTheme(theme)) {
            ImGui.sameLine();
            if (ImGui.smallButton("Save")) {
                this.saveModifiedTheme();
            }
            ImGui.sameLine();
            if (ImGui.smallButton("Revert")) {
                this.modifiedTheme.revertTheme();
                this.modifiedTheme = null;
            }
        }

        ImGui.separator();

        for (ThemeColorCategory category : ThemeColorCategory.values()) {
            List<ThemeColor> colors = this.theme.getColorMap().get(category);

            if (!setOpen) ImGui.setNextItemOpen(true);
            if (ImGui.treeNode(category.getName())) {
                ImGui.separator();

                if (colors != null) for (ThemeColor color : colors) {
                    ImGui.text(color.getLabel());
                    ImGui.sameLine();
                    GuiUtil.smallWidget(() -> {
                        this.colorPicker(theme, color);
                        return false;
                    });
                }

                ImGui.treePop();
            }
        }
    }

    private void saveModifiedTheme() {
        if (Main.getAppDataManager().saveTheme(this.modifiedTheme.getTheme())) {
            this.modifiedTheme = null;
        }
    }

    private void colorPicker(Theme theme, ThemeColor color) {
        final String strId = "###ThemeClrId" + color.getLabel();
        final int flags = ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.NoLabel;
        if (theme.isEditable() ? ImGui.colorEdit4(strId, color.getRgba(), flags) : ImGui.colorButton(strId, color.getRgba(), flags) && theme.isEditable()) {
            if (!this.isModifyingTheme(theme)) this.modifiedTheme = new ModifiedThemeState(theme);
            if (themeManager.getCurrentTheme() == theme) {
                color.getCodeColor().setColor(CodeColorScheme.getRgb(color.getRgba()));
            }
        }
    }

    private boolean isModifyingTheme(Theme theme) {
        return this.modifiedTheme != null && this.modifiedTheme.getTheme() == theme;
    }
}
