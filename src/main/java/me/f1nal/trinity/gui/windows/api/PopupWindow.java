package me.f1nal.trinity.gui.windows.api;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ComponentId;

public abstract class PopupWindow extends AbstractWindow {
    private final String popupId = this.getTitle() + "###" + ComponentId.getId(this.getClass());
    private boolean closeRequested;
    private boolean keyboardInputReady;
    private boolean dismissible = true;

    protected PopupWindow(String title, Trinity trinity) {
        super(title, 0.F, 0.F, trinity);
    }

    @Override
    public final void render() {
        if (!this.closeRequested && !ImGui.isPopupOpen(this.popupId)) {
            ImGui.openPopup(this.popupId);
        }
    }

    public final boolean renderPopup() {
        boolean acceptedKeyboardInput = this.keyboardInputReady;
        if (!this.closeRequested) {
            renderFrame();
            this.keyboardInputReady = true;
        }
        return acceptedKeyboardInput;
    }

    public final void handleEscape() {
        if (this.dismissible) this.onEscape();
    }

    protected void onEscape() {
        this.close();
    }

    public final void setDismissible(boolean dismissible) {
        this.dismissible = dismissible;
    }

    public final boolean isDismissible() {
        return this.dismissible;
    }

    protected final boolean isKeyboardInputReady() {
        return this.keyboardInputReady;
    }

    @Override
    public final void close() {
        this.closeRequested = true;
    }

    public final boolean isCloseRequested() {
        return this.closeRequested;
    }

    public String getPopupId() {
        return popupId;
    }

    public String getStrId(String suffix) {
        return suffix + popupId.replace('#', 'X');
    }
}
