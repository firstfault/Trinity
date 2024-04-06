package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.frames.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.frames.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.frames.impl.constant.search.ConstantSearchTypeString;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class StringComponent extends AbstractPopupTextComponent {
    public StringComponent(String text) {
        super(text);
    }

    @Override
    protected void drawPopup() {
        if (ImGui.menuItem("Copy")) {
            SystemUtil.copyToClipboard(this.getUnquotedText());
        }

        if (ImGui.menuItem("Search All Occurrences...")) {
            Trinity trinity = Main.getTrinity();
            ConstantSearchTypeString constantSearchType = new ConstantSearchTypeString(trinity);
            constantSearchType.getSearchTerm().set(this.getUnquotedText());
            constantSearchType.getExact().set(true);
            List<ConstantViewCache> constantViewList = new ArrayList<>();
            constantSearchType.populate(constantViewList);
            Main.getDisplayManager().addClosableWindow(new ConstantViewFrame(trinity, constantViewList));
        }

        ImGui.endPopup();
    }

    public String getUnquotedText() {
        final String text = this.getText();
        return text.substring(1, text.length() - 1);
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.STRING;
    }
}
