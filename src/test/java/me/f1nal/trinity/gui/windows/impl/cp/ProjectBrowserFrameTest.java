package me.f1nal.trinity.gui.windows.impl.cp;

import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ProjectContainerKind;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.items.PopupItem;
import me.f1nal.trinity.gui.components.popup.items.PopupItemMenu;
import me.f1nal.trinity.gui.components.popup.items.PopupItemMenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

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

    @Test
    void packageMenuGroupsCreationTreeAndCopyActions() {
        Package root = new Package(null);
        Package example = root.createPackage("example");
        example.createPackage("nested");

        PopupItemBuilder popup = example.createPopup();

        assertEquals(List.of("New", "Tree", "Copy", "Rename Package..."), labels(popup.get()));
        assertEquals(List.of("Class...", "Empty File..."), submenuLabels(popup, "New"));
        assertEquals(List.of("Expand All", "Collapse All"), submenuLabels(popup, "Tree"));
        assertEquals(List.of("Name", "Qualified Name", "Internal Path"), submenuLabels(popup, "Copy"));
    }

    @Test
    void archiveRootMenuNeverOffersAnEmptyPathCopy() {
        ProjectContainer container = new ProjectContainer(UUID.randomUUID(), "application.jar",
                ProjectContainerKind.JAR, null);

        PopupItemBuilder popup = container.getRootPackage().createPopup();

        assertEquals(List.of("Edit JAR...", "Export JAR...", "New", "Copy",
                "Rename Archive...", "Remove Archive..."), labels(popup.get()));
        assertEquals(List.of("Name"), submenuLabels(popup, "Copy"));
    }

    private static List<String> submenuLabels(PopupItemBuilder builder, String label) {
        return builder.get().stream()
                .filter(PopupItemMenu.class::isInstance)
                .map(PopupItemMenu.class::cast)
                .filter(menu -> menu.getLabel().equals(label))
                .findFirst()
                .map(PopupItemMenu::getPopupItems)
                .map(ProjectBrowserFrameTest::labels)
                .orElseThrow();
    }

    private static List<String> labels(List<PopupItem> items) {
        return items.stream()
                .map(item -> {
                    if (item instanceof PopupItemMenu menu) return menu.getLabel();
                    if (item instanceof PopupItemMenuItem menuItem) return menuItem.getLabel();
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
