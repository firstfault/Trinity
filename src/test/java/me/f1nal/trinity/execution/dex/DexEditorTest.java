package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import me.f1nal.trinity.application.BrowseService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DexEditorTest {
    @Test
    void rebuildsCompleteDexAroundClassReplacement() throws Exception {
        byte[] original = DexTestFixture.create(Map.of(
                "sample/Edited", "before",
                "sample/Untouched", "keep"));
        DexClassEntry edited = entry(original, "sample/Edited");
        String replacement = edited.disassemble().replace("before", "after");

        DexEditor.Candidate candidate = DexEditor.replaceClass(edited, replacement);

        assertTrue(candidate.valid(), () -> candidate.diagnostics().toString());
        assertEquals(64, candidate.fingerprint().length());
        assertFalse(Arrays.equals(original, candidate.bytes()));
        DexBackedDexFile rebuilt = new DexBackedDexFile(null, candidate.bytes());
        assertEquals(Set.of("Lsample/Edited;", "Lsample/Untouched;"), rebuilt.getClasses().stream()
                .map(ClassDef::getType).collect(Collectors.toSet()));
        assertEquals("after", constant(rebuilt, "Lsample/Edited;"));
        assertEquals("keep", constant(rebuilt, "Lsample/Untouched;"));
    }

    @Test
    void replacesOneMethodAndReportsSmaliLocations() throws Exception {
        DexClassEntry edited = entry(DexTestFixture.create("sample/Edited", "before"),
                "sample/Edited");
        BrowseService.MemberId method = new BrowseService.MemberId(
                "sample/Edited", "run", "()V");
        Method original = edited.findMethod("run", "()V");
        String replacement = DexDisassembler.disassembleMethod(
                edited.getFile(), edited.getClassDef(), original).replace("before", "method-after");

        DexEditor.Candidate candidate = DexEditor.replaceMethod(edited, method, replacement);
        DexEditor.Candidate invalid = DexEditor.replaceMethod(edited, method,
                replacement.replace("return-void", "not-an-opcode"));

        assertTrue(candidate.valid(), () -> candidate.diagnostics().toString());
        assertEquals("method-after", constant(new DexBackedDexFile(null, candidate.bytes()),
                "Lsample/Edited;"));
        assertFalse(invalid.valid());
        assertFalse(invalid.diagnostics().isEmpty());
        assertTrue(invalid.diagnostics().get(0).line() > 0);
        assertTrue(invalid.diagnostics().get(0).column() > 0);
    }

    private static DexClassEntry entry(byte[] bytes, String internalName) {
        DexBackedDexFile dexFile = new DexBackedDexFile(null, bytes);
        DexFileUnit file = new DexFileUnit("classes.dex", bytes, dexFile);
        DexClassEntry target = null;
        for (ClassDef classDef : dexFile.getClasses()) {
            DexClassEntry entry = new DexClassEntry(file, classDef);
            file.addClass(entry);
            if (entry.getInternalName().equals(internalName)) target = entry;
        }
        if (target == null) throw new AssertionError("Missing fixture class " + internalName);
        return target;
    }

    private static String constant(DexBackedDexFile file, String descriptor) {
        ClassDef owner = file.getClasses().stream()
                .filter(classDef -> classDef.getType().equals(descriptor))
                .findFirst().orElseThrow();
        for (Method method : owner.getMethods()) {
            if (method.getImplementation() == null) continue;
            for (Instruction instruction : method.getImplementation().getInstructions()) {
                if (instruction instanceof ReferenceInstruction referenceInstruction
                        && referenceInstruction.getReference() instanceof StringReference string) {
                    return string.getString();
                }
            }
        }
        throw new AssertionError("Missing string constant in " + descriptor);
    }
}
