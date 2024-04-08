package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

import imgui.ImColor;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;

public class DecompilerHighlight {
    private final DecompilerComponent textComponent;
    private final Animation animation = new Animation(Easing.LINEAR, 1550L, 1.F);
    private boolean scrolled;

    public DecompilerHighlight(DecompilerComponent textComponent) {
        this.textComponent = textComponent;
    }

    public void setScrolled(boolean scrolled) {
        this.scrolled = scrolled;
    }

    public boolean isScrolled() {
        return scrolled;
    }

    public DecompilerComponent getTextComponent() {
        return textComponent;
    }

    public boolean isFinished() {
        return this.animation.isFinished();
    }

    public int getColor() {
        animation.run(0.F);
        int white = (int) (150.F * animation.getValue());
        return ImColor.rgba(white, white, white, (int) (120.F * animation.getValue()));
    }
}
