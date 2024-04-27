package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types;

import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;

public abstract class FullRenameMember {
    private final String label, namePrefix;
    private final MemorableCheckboxComponent enabled;
    private int count;

    protected FullRenameMember(String label, String namePrefix) {
        this.label = label;
        this.namePrefix = namePrefix;
        this.enabled = new MemorableCheckboxComponent("fullRename" + label, true);
    }

    public abstract void refactor(GlobalRenameContext context);

    public final void prepare() {
        this.count = 0;
    }

    public final void draw() {
        this.enabled.drawCheckbox(this.getLabel());
    }

    protected final String generateName() {
        return generateName(++this.count);
    }

    protected final String generateName(int count) {
        return this.namePrefix + count;
    }

    public final boolean isEnabled() {
        return enabled.getState();
    }

    public final String getLabel() {
        return label;
    }
}
