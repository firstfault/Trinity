package me.f1nal.trinity.database.inputs;

import org.objectweb.asm.ClassReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fast validation performed before a new project starts mutating runtime state. */
public final class ProjectInputValidator {
    private ProjectInputValidator() {
    }

    public static List<String> validate(ProjectInputSet input) {
        List<String> problems = new ArrayList<>();
        Map<String, String> classOwners = new HashMap<>();
        for (ProjectContainerInput container : input.getContainers()) {
            for (UnreadClassBytes unread : container.getClassPath().getClasses()) {
                String className;
                try {
                    className = new ClassReader(unread.getBytes()).getClassName();
                } catch (Throwable throwable) {
                    problems.add(container.getName() + ": could not parse " + unread.getEntryName());
                    continue;
                }
                String previous = classOwners.putIfAbsent(className, container.getName());
                if (previous != null) {
                    problems.add(container.getName() + ": duplicate class " + className
                            + " (already supplied by " + previous + ")");
                }
            }
        }
        return problems;
    }
}
