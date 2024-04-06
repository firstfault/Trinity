package me.f1nal.trinity.events;

import me.f1nal.trinity.execution.ClassTarget;

public class EventMemberRename {
    private final ClassTarget target;

    public EventMemberRename(ClassTarget target) {
        this.target = target;
    }

    public ClassTarget getTarget() {
        return target;
    }
}
