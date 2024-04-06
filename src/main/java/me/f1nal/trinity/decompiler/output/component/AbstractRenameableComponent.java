package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.util.NameUtil;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiMouseCursor;
import imgui.internal.flag.ImGuiItemFlags;
import imgui.type.ImString;

public abstract class AbstractRenameableComponent extends AbstractTextComponent {
    private boolean renaming, justRenamed;
    private ImString renameText;
    private boolean refreshNeeded;
    
    public AbstractRenameableComponent(String text) {
        super(text);
    }

    @Override
    public boolean handleItemHover() {
        if (ImGui.isMouseClicked(1)) {
            ImGui.openPopup(this.getPopupId());
            return true;
        }
        if (ImGui.getIO().getKeyCtrl()) {
            if (isRenameable() && !renaming) {
                ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);
                if (ImGui.isMouseClicked(0)) {
                    ImGui.openPopup(this.getPopupId());
                    this.beginRenaming();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void handleAfterDrawing() {
        super.handleAfterDrawing();
        if (this.justRenamed) {
            ImGui.openPopup(this.getPopupId());
        }
        if (ImGui.beginPopup(this.getPopupId(), ImGuiItemFlags.SelectableDontClosePopup)) {
            this.showPopup();
        } else {
            this.renaming = false;
        }
        if (refreshNeeded) {
            notifyRefreshed();
            refreshNeeded = false;
        }
    }

    public void setRefreshNeeded(boolean refreshNeeded) {
        this.refreshNeeded = refreshNeeded;
    }

    protected abstract void showPopup();

    protected String getPopupId() {
        return "ARC".concat(String.valueOf(getId()));
    }
    
    public abstract String getName();
    protected abstract void rename(String newName);
    public abstract boolean isRenameable();
    protected abstract void notifyRefreshed();
    
    protected boolean showRenamingItem() {
        ImGui.text(NameUtil.cleanNewlines(this.renaming ? "Renaming: " + this.getName() : this.getName()));
        ImGui.separator();
        if (isRenameable()) {
            if (this.renaming) {
                if (this.justRenamed) {
                    ImGui.setKeyboardFocusHere();
                    this.justRenamed = false;
                }
                if (ImGui.inputText("Name", renameText, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    Main.getEventBus().post(new EventRefreshDecompilerText(dc -> dc.containsComponent(this)));
                    this.rename(renameText.get());
                    ImGui.closeCurrentPopup();
                    this.renaming = this.justRenamed = false;
                    this.refreshNeeded = true;
                }
                ImGui.endPopup();
                return true;
            } else {
                if (ImGui.menuItem("Rename", "R")) {
                    this.beginRenaming();
                }
            }
        }
        return false;
    }

    private void beginRenaming() {
        this.renaming = true;
        this.justRenamed = true;
        this.renameText = new ImString(this.getName(), 64);
    }
}
