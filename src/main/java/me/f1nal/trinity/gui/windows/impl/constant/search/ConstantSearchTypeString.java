package me.f1nal.trinity.gui.windows.impl.constant.search;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.util.GuiUtil;
import me.f1nal.trinity.util.SearchTermMatchable;

import java.util.List;
import java.util.Objects;

public class ConstantSearchTypeString extends ConstantSearchType {
    private final ImBoolean caseInsensitive = new ImBoolean(true);
    private final ImBoolean exact = new ImBoolean(true);
    private final ImString searchTerm = new ImString(0x500);

    public ConstantSearchTypeString(Trinity trinity) {
        super("String", trinity);
    }

    public ImBoolean getExact() {
        return exact;
    }

    public ImString getSearchTerm() {
        return searchTerm;
    }

    @Override
    public boolean draw() {
        ImGui.inputText("Search Term", this.searchTerm);
        GuiUtil.smallCheckbox("Case Insensitive", this.caseInsensitive);
        ImGui.sameLine();
        GuiUtil.smallCheckbox("Exact", this.exact);
        GuiUtil.tooltip("If set, the string must exactly match the input instead of containing it.");
        return true;
    }

    private SearchTermMatchable createMatchable() {
        String inputSearch = this.searchTerm.get();

        if (inputSearch.isEmpty()) {
            return searchTerm -> true;
        }

        // Exact Search
        if (this.exact.get()) {
            if (this.caseInsensitive.get()) return searchTerm -> searchTerm.equalsIgnoreCase(inputSearch);
            return searchTerm -> searchTerm.equals(inputSearch);
        }

        // Containing Search
        if (this.caseInsensitive.get()) {
            final String lowerCasedSearch = inputSearch.toLowerCase();
            return searchTerm -> searchTerm.toLowerCase().contains(lowerCasedSearch);
        }
        return searchTerm -> searchTerm.contains(inputSearch);
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        SearchTermMatchable matchable = Objects.requireNonNull(this.createMatchable(), "String matcher");

        new LdcConstantSearcher<String>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof String && matchable.matches((String) value);
            }

            @Override
            protected String convertConstantToText(String value) {
                return String.format("\"%s\"", value);
            }
        }.populate(list, getTrinity().getExecution());
    }

}
