package me.f1nal.trinity.execution.packages;

import me.f1nal.trinity.database.Database;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProjectContainerTest {
    @Test
    void resourcePathsAreScopedPerContainer() {
        Database database = new Database("test", new File("test.tdb"), null);
        ProjectContainer first = new ProjectContainer(UUID.randomUUID(), "first.jar", ProjectContainerKind.JAR, database);
        ProjectContainer second = new ProjectContainer(UUID.randomUUID(), "second.jar", ProjectContainerKind.JAR, database);
        ResourceArchiveEntry firstResource = new ResourceArchiveEntry("config.json", new byte[]{1});
        ResourceArchiveEntry secondResource = new ResourceArchiveEntry("config.json", new byte[]{2});

        first.register(firstResource);
        second.register(secondResource);

        assertSame(firstResource, first.getResource("config.json"));
        assertSame(secondResource, second.getResource("config.json"));
        assertNotEquals(firstResource, secondResource);
        assertThrows(IllegalArgumentException.class,
                () -> first.register(new ResourceArchiveEntry("config.json", new byte[]{3})));
    }
}
