package me.f1nal.trinity.remap;

import java.util.Objects;

public class DisplayName {
    private String name;
    private RenameType type;

    public DisplayName(String name) {
        this.name = name;
        this.type = RenameType.NONE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RenameType getType() {
        return type;
    }

    public void setType(RenameType type) {
        this.type = type;
    }
}
