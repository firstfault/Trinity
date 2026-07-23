package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ProjectInputSet {
    public static final String LOOSE_FILES_NAME = "Loose Files";

    private final List<ProjectContainerInput> containers = new ArrayList<>();

    public void addJar(String name, ClassPath classPath) {
        containers.add(new ProjectContainerInput(UUID.randomUUID(), name, ProjectContainerKind.JAR, classPath));
    }

    public void addLoose(ClassPath classPath) {
        ProjectContainerInput loose = containers.stream()
                .filter(input -> input.getKind() == ProjectContainerKind.LOOSE)
                .findFirst()
                .orElseGet(() -> {
                    ProjectContainerInput created = new ProjectContainerInput(UUID.randomUUID(), LOOSE_FILES_NAME,
                            ProjectContainerKind.LOOSE, new ClassPath());
                    containers.add(created);
                    return created;
                });
        loose.getClassPath().addClassPath(classPath);
    }

    public void add(ProjectContainerInput input) {
        containers.add(input);
    }

    public List<ProjectContainerInput> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    public boolean isEmpty() {
        return containers.isEmpty();
    }
}
