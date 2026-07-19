package me.f1nal.trinity.gui.windows.impl.assembler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssemblerValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String message) {
        errors.add(message);
    }

    public void warning(String message) {
        warnings.add(message);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
