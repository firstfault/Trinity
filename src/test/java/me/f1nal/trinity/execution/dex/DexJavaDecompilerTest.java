package me.f1nal.trinity.execution.dex;

import me.f1nal.trinity.application.BrowseService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DexJavaDecompilerTest {
    @Test
    void producesJavaLikeClassAndMethodSourceFromMemory() throws Exception {
        byte[] dex = DexTestFixture.create("sample/DexMain", "native-dex");

        DexJavaDecompiler.ClassView view = DexJavaDecompiler.decompile(
                dex, "classes.dex", "sample/DexMain");
        String method = DexJavaDecompiler.methodSource(view,
                new BrowseService.MemberId("sample/DexMain", "run", "()V"));

        assertTrue(view.source().contains("class DexMain"), view::source);
        assertTrue(view.source().contains("package sample;"), view::source);
        assertTrue(method.contains("run()"), () -> method);
        assertEquals(0, view.errorCount());
    }

    @Test
    void repairsDexNamesThatAreNotValidJavaIdentifiers() throws Exception {
        DexJavaDecompiler.ClassView view = DexJavaDecompiler.decompile(
                DexTestFixture.create("sample/3Bad", "native-dex"),
                "classes.dex", "sample/3Bad");

        assertTrue(view.source().contains("class C3Bad"), view::source);
    }

    @Test
    void repairsReferencesResolvedFromAnotherDexFile() throws Exception {
        DexJavaDecompiler.ClassView view = DexJavaDecompiler.decompile(List.of(
                        new DexJavaDecompiler.Input("classes.dex",
                                DexTestFixture.createWithField("sample/Main", "sample/3Bad")),
                        new DexJavaDecompiler.Input("classes2.dex",
                                DexTestFixture.create("sample/3Bad", "native-dex"))),
                "sample/Main");

        assertTrue(view.source().contains("C3Bad dependency;"), view::source);
    }

    @Test
    void reusesOneModelUntilTheDexSnapshotChanges() throws Exception {
        List<DexJavaDecompiler.Input> first = List.of(new DexJavaDecompiler.Input(
                "classes.dex", DexTestFixture.create("sample/First", "first")));
        List<DexJavaDecompiler.Input> second = List.of(new DexJavaDecompiler.Input(
                "classes.dex", DexTestFixture.create("sample/Second", "second")));

        try (DexJavaDecompiler.Workspace workspace = new DexJavaDecompiler.Workspace()) {
            workspace.decompile(first, "sample/First");
            workspace.decompile(first, "sample/First");
            assertEquals(1, workspace.modelBuildCount());

            workspace.decompile(second, "sample/Second");
            assertEquals(2, workspace.modelBuildCount());
        }
    }

    @Test
    void sharesOneModelAcrossConcurrentRequests() throws Exception {
        List<DexJavaDecompiler.Input> inputs = List.of(new DexJavaDecompiler.Input(
                "classes.dex", DexTestFixture.create("sample/Concurrent", "shared")));

        try (DexJavaDecompiler.Workspace workspace = new DexJavaDecompiler.Workspace()) {
            CompletableFuture<DexJavaDecompiler.ClassView> first = CompletableFuture.supplyAsync(
                    () -> workspace.decompile(inputs, "sample/Concurrent"));
            CompletableFuture<DexJavaDecompiler.ClassView> second = CompletableFuture.supplyAsync(
                    () -> workspace.decompile(inputs, "sample/Concurrent"));

            CompletableFuture.allOf(first, second).join();
            assertTrue(first.join().source().contains("class Concurrent"));
            assertTrue(second.join().source().contains("class Concurrent"));
            assertEquals(1, workspace.modelBuildCount());
        }
    }
}
