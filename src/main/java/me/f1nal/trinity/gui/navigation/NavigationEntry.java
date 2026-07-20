package me.f1nal.trinity.gui.navigation;

import me.f1nal.trinity.Trinity;

public record NavigationEntry(long id, NavigationTarget target, NavigationAction action) {
    public String describe(Trinity trinity) {
        return action.getHistoryPrefix() + " " + target.describe(trinity);
    }
}
