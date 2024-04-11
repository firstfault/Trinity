package me.f1nal.trinity.events;

import me.f1nal.trinity.execution.ClassInput;

public class EventClassModified {
    private final ClassInput classInput;

    public EventClassModified(ClassInput classInput) {
        this.classInput = classInput;
    }

    public ClassInput getClassInput() {
        return classInput;
    }
}
