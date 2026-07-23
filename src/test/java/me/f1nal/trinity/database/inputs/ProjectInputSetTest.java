package me.f1nal.trinity.database.inputs;

import me.f1nal.trinity.database.ClassPath;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectInputSetTest {
    @Test
    void keepsJarsSeparateAndGroupsRawClasses() {
        ProjectInputSet inputs = new ProjectInputSet();
        ClassPath firstJar = classPath("a/A.class");
        ClassPath secondJar = classPath("b/B.class");
        ClassPath firstLoose = classPath("LooseA.class");
        ClassPath secondLoose = classPath("LooseB.class");

        inputs.addJar("first.jar", firstJar);
        inputs.addJar("second.jar", secondJar);
        inputs.addLoose(firstLoose);
        inputs.addLoose(secondLoose);

        assertEquals(3, inputs.getContainers().size());
        assertEquals(ProjectContainerKind.JAR, inputs.getContainers().get(0).getKind());
        assertEquals(ProjectContainerKind.JAR, inputs.getContainers().get(1).getKind());
        ProjectContainerInput loose = inputs.getContainers().get(2);
        assertEquals(ProjectContainerKind.LOOSE, loose.getKind());
        assertEquals(ProjectInputSet.LOOSE_FILES_NAME, loose.getName());
        assertEquals(2, loose.getClassPath().getClasses().size());
    }

    private static ClassPath classPath(String name) {
        ClassPath classPath = new ClassPath();
        classPath.addClass(new UnreadClassBytes(name, new byte[]{1}));
        return classPath;
    }
}
