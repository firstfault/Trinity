package me.f1nal.trinity.gui.frames.impl.classstructure.nodes;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.gui.components.ComponentId;
import me.f1nal.trinity.gui.components.events.MouseClickType;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.tree.GenericTreeNode;
import me.f1nal.trinity.gui.frames.impl.classstructure.StructureKind;
import me.f1nal.trinity.gui.frames.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.frames.impl.cp.IBrowserViewerNode;
import me.f1nal.trinity.gui.frames.impl.cp.RenameHandler;

public abstract class ClassStructureNode extends GenericTreeNode<ClassStructureNode> implements IBrowserViewerNode {
    private BrowserViewerNode browserViewerNode;
    private final String strId = ComponentId.getId(this.getClass());
    private final String icon;

    protected ClassStructureNode(String icon) {
        this.icon = icon;
    }

    protected BrowserViewerNode createBrowserViewerNode() {
        BrowserViewerNode node = new BrowserViewerNode(this.icon, () -> this.getKind().getColor(), this::getText, getRenameFunction());
        node.addMouseClickHandler(clickType -> {
            if (clickType == MouseClickType.RIGHT_CLICK) {
                PopupItemBuilder popup = PopupItemBuilder.create();

                if (node.isRenameAvailable()) popup.menuItem("Rename", node::beginRenaming);
                this.populatePopup(popup);

                Main.getDisplayManager().showPopup(popup);
            } else if (clickType == MouseClickType.LEFT_CLICK) {
                this.handleLeftClick();
            }
        });
        return node;
    }

    @Override
    public boolean matches(String searchTerm) {
        return getText().contains(searchTerm);
    }

    protected void handleLeftClick() {

    }

    protected void populatePopup(PopupItemBuilder popup) {

    }

    protected abstract RenameHandler getRenameFunction();

    protected abstract String getText();

    public final String getStrId() {
        return strId;
    }

    @Override
    public abstract StructureKind getKind();

    @Override
    public final BrowserViewerNode getBrowserViewerNode() {
        if (browserViewerNode == null) {
            browserViewerNode = this.createBrowserViewerNode();
        }
        return browserViewerNode;
    }
}
