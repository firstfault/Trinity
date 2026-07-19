package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImColor;

public class DecompilerHighlight {
    private static final long DURATION = 700L;
    private static final long FADE_IN_DURATION = 90L;
    private final DecompilerLine line;
    private final long startTime = System.currentTimeMillis();

    public DecompilerHighlight(DecompilerLine line) {
        this.line = line;
    }

    public DecompilerLine getLine() {
        return line;
    }

    public boolean isFinished() {
        return getElapsed() >= DURATION;
    }

    public int getFillColor() {
        return ImColor.rgba(105, 105, 105, Math.round(62.F * getOpacity()));
    }

    public int getBorderColor() {
        return ImColor.rgba(165, 165, 165, Math.round(170.F * getOpacity()));
    }

    private float getOpacity() {
        long elapsed = getElapsed();
        if (elapsed < FADE_IN_DURATION) {
            float progress = (float) elapsed / FADE_IN_DURATION;
            return 1.F - (1.F - progress) * (1.F - progress);
        }
        return Math.max(0.F, 1.F - (float) (elapsed - FADE_IN_DURATION) / (DURATION - FADE_IN_DURATION));
    }

    private long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }
}
