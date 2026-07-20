package me.f1nal.trinity.gui.windows.impl.cp;

import me.f1nal.trinity.execution.packages.Package;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectBrowserFrameTest {
    @Test
    void compactsSingleChildPackageChainsUntilABranch() {
        Package root = new Package(null);
        Package me = root.createPackage("me");
        Package example = me.createPackage("example");
        Package test = example.createPackage("test");
        test.createPackage("client");
        test.createPackage("server");

        List<Package> chain = ProjectBrowserFrame.createCompactPackageChain(me);

        assertEquals(List.of(me, example, test), chain);
    }
}
