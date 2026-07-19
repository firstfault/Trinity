package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.GuiUtil;

public class SearchBar {
    private final ImString searchText;
    private final String id = ComponentId.getId(getClass());
    private boolean caseSensitive;
    public int inputTextFlags = ImGuiInputTextFlags.None;

    public SearchBar(int length) {
        this.searchText = new ImString(length);
    }

    public SearchBar() {
        this(256);
    }

    public String drawAndGet() {
        draw();
        return this.searchText.get();
    }

    public boolean draw() {
        return ImGui.inputText("###Search" + id, searchText, inputTextFlags);
    }

    public boolean drawWithCaseSensitivityToggle() {
        float buttonSize = ImGui.getFrameHeight();
        float spacing = ImGui.getStyle().getItemSpacingX();
        if (this.caseSensitive) {
            ImGui.pushStyleColor(ImGuiCol.Button,
                    CodeColorScheme.setAlpha(Main.getPreferences().getAccentColor().getColor(), 92));
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button,
                    CodeColorScheme.setAlpha(CodeColorScheme.WIDGET_BACKGROUND, 255));
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered,
                    CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 72));
            ImGui.pushStyleColor(ImGuiCol.ButtonActive,
                    CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 100));
        }
        IconFamily.CODICON.pushFont();
        boolean toggle = ImGui.button(CodiconIcons.CASE_SENSITIVE + "###CaseSensitive" + id,
                buttonSize, buttonSize);
        IconFamily.CODICON.popFont();
        ImGui.popStyleColor(this.caseSensitive ? 1 : 3);
        GuiUtil.tooltip("Case Sensitive");

        ImGui.sameLine(0.F, spacing);
        ImGui.setNextItemWidth(Math.max(1.F, ImGui.getContentRegionAvailX()));
        boolean changed = this.draw();

        if (toggle) {
            this.caseSensitive = !this.caseSensitive;
            changed = true;
        }
        return changed;
    }

    public String getText() {
        return getSearchText().get();
    }

    public ImString getSearchText() {
        return searchText;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}
