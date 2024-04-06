package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.theme.CodeColorScheme;

public class RawTextComponent extends AbstractTextComponent {
    public RawTextComponent(String text) {
        super(text);
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.TEXT;
    }

    @Override
    public boolean handleItemHover() {
        return false;
    }
}
