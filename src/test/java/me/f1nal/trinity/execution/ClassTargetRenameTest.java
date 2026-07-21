package me.f1nal.trinity.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassTargetRenameTest {
    @Test
    void classRenameOnlyReplacesSimpleName() {
        ClassTarget target = new ClassTarget("me/example/testproject/packagee/OriginalName", 0);

        assertEquals("OriginalName", target.getRenameHandler().getFullName());
        assertEquals("me/example/testproject/packagee/RenamedClass",
                target.getNameInCurrentPackage("RenamedClass"));
        assertEquals("me/example/testproject/packagee/RenamedClass",
                target.getNameInCurrentPackage("ignored/package/RenamedClass"));
    }

    @Test
    void classRenameKeepsDefaultPackage() {
        ClassTarget target = new ClassTarget("OriginalName", 0);

        assertEquals("RenamedClass", target.getNameInCurrentPackage("RenamedClass"));
    }
}
