package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;

public class SearchBar {
    private final ImString searchText;
    private final String id = ComponentId.getId(getClass());
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
        return ImGui.inputText("Search###" + id, searchText, inputTextFlags);
    }

    public String getText() {
        return getSearchText().get();
    }

    public ImString getSearchText() {
        return searchText;
    }
}
