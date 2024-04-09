package me.f1nal.trinity.gui.windows.impl.project.create;

import me.f1nal.trinity.gui.components.tabs.TabFrame;
import me.f1nal.trinity.util.IDescribable;

public abstract class AbstractProjectCreationTab implements TabFrame, IDescribable {
    protected abstract boolean isInputValid();
}
