package me.f1nal.trinity.database.object;

import me.f1nal.trinity.gui.navigation.NavigationEntry;

public final class DatabaseNavigationEntry {
    private final long id;
    private final String className;
    private final String inputType;
    private final String memberName;
    private final String memberDescriptor;
    private final int instructionIndex;
    private final String action;
    private final long timestampMillis;
    private final String displayText;

    public DatabaseNavigationEntry(NavigationEntry entry) {
        this.id = entry.id();
        this.className = entry.target().getClassTarget().getRealName();
        this.inputType = entry.target().getInputType().name();
        this.memberName = entry.target().getMemberName();
        this.memberDescriptor = entry.target().getMemberDescriptor();
        this.instructionIndex = entry.target().getInstructionIndex();
        this.action = entry.action().name();
        this.timestampMillis = entry.timestampMillis();
        this.displayText = entry.displayText();
    }

    public long getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getInputType() {
        return inputType;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberDescriptor() {
        return memberDescriptor;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public String getAction() {
        return action;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getDisplayText() {
        return displayText;
    }
}
