package me.f1nal.trinity.gui.windows.impl.cp;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.IconFamily;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.filter.kind.IKindType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.Locale;

final class ProjectBrowserMemberNode implements IBrowserViewerNode {
    private final MemberInput<?> input;
    private final BrowserViewerNode browserViewerNode;

    ProjectBrowserMemberNode(MemberInput<?> input) {
        this.input = input;
        boolean method = input instanceof MethodInput;
        this.browserViewerNode = new BrowserViewerNode(method ? CodiconIcons.SYMBOL_METHOD : CodiconIcons.SYMBOL_FIELD,
                IconFamily.CODICON,
                () -> method ? CodeColorScheme.METHOD_REF : CodeColorScheme.FIELD_REF,
                () -> input.getDisplayName().getName(),
                input);
        this.browserViewerNode.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                PopupItemBuilder popup = PopupItemBuilder.create();
                input.populatePopup(popup);
                Main.getDisplayManager().showPopup(popup);
            } else if (clickType == MouseClickType.LEFT_CLICK) {
                Main.getDisplayManager().openDecompilerView(input);
            }
        });
    }

    MemberInput<?> getInput() {
        return input;
    }

    @Override
    public BrowserViewerNode getBrowserViewerNode() {
        return browserViewerNode;
    }

    @Override
    public IKindType getKind() {
        return null;
    }

    @Override
    public boolean matches(String searchTerm) {
        return input.getDisplayName().getName().contains(searchTerm) || input.getDescriptor().contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        String normalized = searchTerm.toLowerCase(Locale.ROOT);
        return input.getDisplayName().getName().toLowerCase(Locale.ROOT).contains(normalized)
                || input.getDescriptor().toLowerCase(Locale.ROOT).contains(normalized);
    }
}
