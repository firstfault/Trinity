package me.f1nal.trinity.gui.frames.impl.xref;

import me.f1nal.trinity.util.INameable;

public enum SearchMaxDisplay implements INameable {
    MAX_100(100),
    MAX_200(200),
    MAX_500(500),
    MAX_1000(1000),
    MAX_2000(2000),
    MAX_2500(2500),
    ;

    private final int max;

    SearchMaxDisplay(int max) {
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    @Override
    public String getName() {
        return String.valueOf(this.max);
    }
}
