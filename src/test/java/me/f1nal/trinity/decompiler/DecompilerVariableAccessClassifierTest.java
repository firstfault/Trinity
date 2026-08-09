package me.f1nal.trinity.decompiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DecompilerVariableAccessClassifierTest {
    @Test
    void classifiesReadsWritesAndMutations() {
        assertAccess("consume(value);", "value", false, false,
                DecompilerVariableAccess.READ);
        assertAccess("value = source;", "value", false, false,
                DecompilerVariableAccess.WRITE);
        assertAccess("value += amount;", "value", false, false,
                DecompilerVariableAccess.READ_WRITE);
        assertAccess("++value;", "value", false, false,
                DecompilerVariableAccess.READ_WRITE);
        assertAccess("value--;", "value", false, false,
                DecompilerVariableAccess.READ_WRITE);
        assertAccess("if (value == other) {}", "value", false, false,
                DecompilerVariableAccess.READ);
        assertAccess("values[index] = value;", "values", false, false,
                DecompilerVariableAccess.READ);
    }

    @Test
    void omitsNonRuntimeDeclarationsAndKeepsImplicitWrites() {
        assertAccess("void run(int value) {", "value", true, true, null);
        assertAccess("int value;", "value", true, false, null);
        assertAccess("int value, other;", "value", true, false, null);
        assertAccess("int value = source;", "value", true, false,
                DecompilerVariableAccess.WRITE);
        assertAccess("for (String value : values) {", "value", true, false,
                DecompilerVariableAccess.WRITE);
        assertAccess("catch (Exception value) {", "value", true, false,
                DecompilerVariableAccess.WRITE);
    }

    private static void assertAccess(String line, String variable, boolean declaration,
                                     boolean methodSignature,
                                     DecompilerVariableAccess expected) {
        int start = line.indexOf(variable);
        DecompilerVariableAccess actual = DecompilerVariableAccessClassifier.classify(
                line, start, start + variable.length(), declaration, methodSignature);
        if (expected == null) {
            assertNull(actual);
        } else {
            assertEquals(expected, actual);
        }
    }
}
