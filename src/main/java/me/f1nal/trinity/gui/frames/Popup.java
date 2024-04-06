package me.f1nal.trinity.gui.frames;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.ComponentId;

public abstract class Popup extends Frame {
    private final String popupId = this.getTitle() + "###" + ComponentId.getId(this.getClass());
    private boolean setVisible;
    private Runnable closeEvent;
    protected boolean closeOnEscape = true;

    protected Popup(String title, Trinity trinity) {
        super(title, 0.F, 0.F, trinity);
    }

    @Override
    public void render() {
        if (!setVisible) {
            ImGui.openPopup(this.popupId);
            setVisible = true;
        }
    }

    public void renderPopup() {
        renderFrame();
    }

    public boolean canCloseOnEscapeNow() {
        return closeOnEscape;
    }

    public void close() {
        if (this.closeEvent != null) {
            this.closeEvent.run();
        }
    }

    public Runnable getCloseEvent() {
        return closeEvent;
    }

    public void setCloseEvent(Runnable closeEvent) {
        this.closeEvent = closeEvent;
    }

    public String getPopupId() {
        return popupId;
    }

    public String getStrId(String suffix) {
        return suffix + popupId.replace('#', 'X');
    }
}
