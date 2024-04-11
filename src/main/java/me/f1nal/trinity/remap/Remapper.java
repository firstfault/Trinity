package me.f1nal.trinity.remap;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.*;

public final class Remapper {
    /**
     * Program execution flow
     */
    private final Execution execution;
    private final ObfuscationAnalysis obfuscationAnalysis = new ObfuscationAnalysis();

    public Remapper(Execution execution) {
        this.execution = execution;
    }

    public void renameClass(ClassTarget target, String newName) {
        target.setDisplayName(newName);
        target.setPackage(execution.getRootPackage());
        target.save();
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public void renameMethod(MethodInput methodInput, String newName) {
        methodInput.setDisplayName(newName);
        methodInput.save();
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public void renameField(FieldInput fieldInput, String newName) {
        fieldInput.setDisplayName(newName);
        fieldInput.save();
        Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
    }

    public <I extends Input> void rename(I input, String newName) {
        input.rename(this, newName);
    }

    public ObfuscationAnalysis getObfuscationAnalysis() {
        return obfuscationAnalysis;
    }

    public Execution getExecution() {
        return execution;
    }
}
