package me.f1nal.trinity.database.object;

import me.f1nal.trinity.gui.navigation.NavigationEntry;
import me.f1nal.trinity.gui.navigation.NavigationViewState;

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
    private final boolean hasViewState;
    private final int cursorLine;
    private final int cursorCharacter;
    private final int selectionLine;
    private final int selectionCharacter;
    private final boolean selectionUsesBoundaries;
    private final float scrollX;
    private final float scrollY;
    private final boolean importsExpanded;

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
        NavigationViewState viewState = entry.viewState();
        this.hasViewState = viewState != null;
        this.cursorLine = viewState == null ? -1 : viewState.cursorLine();
        this.cursorCharacter = viewState == null ? 0 : viewState.cursorCharacter();
        this.selectionLine = viewState == null ? -1 : viewState.selectionLine();
        this.selectionCharacter = viewState == null ? 0 : viewState.selectionCharacter();
        this.selectionUsesBoundaries = viewState != null && viewState.selectionUsesBoundaries();
        this.scrollX = viewState == null ? 0.F : viewState.scrollX();
        this.scrollY = viewState == null ? 0.F : viewState.scrollY();
        this.importsExpanded = viewState != null && viewState.importsExpanded();
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

    public NavigationViewState getViewState() {
        if (!hasViewState) return null;
        return new NavigationViewState(cursorLine, cursorCharacter,
                selectionLine, selectionCharacter, selectionUsesBoundaries,
                scrollX, scrollY, importsExpanded);
    }
}
