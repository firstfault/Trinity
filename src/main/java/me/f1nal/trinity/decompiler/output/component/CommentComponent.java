package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.theme.CodeColorScheme;
import imgui.ImGui;

public class CommentComponent extends AbstractPopupTextComponent {
    public CommentComponent(String text) {
        super(text);
    }

    @Override
    protected void drawPopup() {
        if (ImGui.menuItem("Hide all comments")) {
            Main.getPreferences().setDecompilerHideComments(true);
            Main.getDisplayManager().getArchiveEntryViewerFacade().resetDecompilerComponents();
        }

        ImGui.endPopup();
    }

    @Override
    public int getTextColor() {
        return CodeColorScheme.DISABLED;
    }

    @Override
    public String getText() {
        return Main.getPreferences().isDecompilerHideComments() ? "" : super.getText();
    }
}
