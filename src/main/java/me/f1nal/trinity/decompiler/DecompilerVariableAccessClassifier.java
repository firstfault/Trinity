package me.f1nal.trinity.decompiler;

import org.jetbrains.annotations.Nullable;

/** Classifies a rendered variable occurrence using the Java operators around it. */
public final class DecompilerVariableAccessClassifier {
    private static final String[] COMPOUND_ASSIGNMENTS = {
            ">>>=", ">>=", "<<=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^="
    };

    private DecompilerVariableAccessClassifier() {
    }

    public static @Nullable DecompilerVariableAccess classify(String line, int variableStart,
                                                               int variableEnd, boolean declaration,
                                                               boolean methodSignature) {
        int start = Math.max(0, Math.min(variableStart, line.length()));
        int end = Math.max(start, Math.min(variableEnd, line.length()));
        String before = line.substring(0, start).stripTrailing();
        String after = line.substring(end).stripLeading();

        if (before.endsWith("++") || before.endsWith("--")
                || after.startsWith("++") || after.startsWith("--")) {
            return DecompilerVariableAccess.READ_WRITE;
        }
        for (String operator : COMPOUND_ASSIGNMENTS) {
            if (after.startsWith(operator)) return DecompilerVariableAccess.READ_WRITE;
        }
        if (after.startsWith("=") && !after.startsWith("==")) {
            return DecompilerVariableAccess.WRITE;
        }

        if (declaration) {
            // Method parameters and declarations without an initializer are not runtime accesses.
            if (methodSignature || after.startsWith(";") || after.startsWith(",")) return null;
            // Catch, enhanced-for, pattern, and resource variables receive a value implicitly.
            return DecompilerVariableAccess.WRITE;
        }
        return DecompilerVariableAccess.READ;
    }
}
