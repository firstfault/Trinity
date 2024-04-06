package me.f1nal.trinity.gui.viewport.notifications;

public class SimpleCaption implements ICaption {
    private final String caption;

    public SimpleCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public String getCaption() {
        return caption;
    }
}
