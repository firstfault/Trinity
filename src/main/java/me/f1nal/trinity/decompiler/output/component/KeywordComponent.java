package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.theme.CodeColorScheme;

public class KeywordComponent extends AbstractTextComponent {
    public KeywordComponent(String text) {
        super(text);
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.KEYWORD;
    }

    @Override
    public boolean handleItemHover() {
        return false;
    }
}
