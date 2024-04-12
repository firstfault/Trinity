package me.f1nal.trinity.remap;

import java.util.Objects;

public class DisplayName {
    private final String originalName;
    private String name;
    private RenameType type;

    public DisplayName(String name) {
        this.originalName = name;
        this.name = name;
        this.type = RenameType.NONE;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name, RenameType renameType) {
        if (name.equals(this.originalName)) {
            renameType = RenameType.NONE;
        } else if (renameType == null) {
            renameType = RenameType.MANUAL;
        }

        this.setNameFinally(name);
        this.setType(renameType);
    }

    protected void setNameFinally(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.setName(name, RenameType.MANUAL);
    }

    public RenameType getType() {
        return type;
    }

    public void setType(RenameType type) {
        this.type = Objects.requireNonNull(type, "Rename type cannot be null");
    }
}
