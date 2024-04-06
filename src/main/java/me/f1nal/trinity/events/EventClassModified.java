package me.f1nal.trinity.events;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.util.ModifyPriority;

public class EventClassModified {
    private final ClassInput classInput;
    private final ModifyPriority priority;

    public EventClassModified(ClassInput classInput, ModifyPriority priority) {
        this.classInput = classInput;
        this.priority = priority;
    }

    public ClassInput getClassInput() {
        return classInput;
    }

    public ModifyPriority getPriority() {
        return priority;
    }
}
