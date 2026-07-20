package me.f1nal.trinity.database.object;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.navigation.NavigationEntry;
import me.f1nal.trinity.gui.navigation.NavigationTarget;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DatabaseNavigationHistory extends AbstractDatabaseObject {
    private final List<DatabaseNavigationEntry> entries;
    private final int currentIndex;

    public DatabaseNavigationHistory(List<NavigationEntry> entries, int currentIndex) {
        this.entries = new ArrayList<>(entries.size());
        entries.stream().map(DatabaseNavigationEntry::new).forEach(this.entries::add);
        this.currentIndex = currentIndex;
    }

    @Override
    public boolean load(Trinity trinity) {
        List<NavigationEntry> restoredEntries = new ArrayList<>();
        int restoredCurrentIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            NavigationEntry restored = restoreEntry(trinity, entries.get(i));
            if (restored == null) continue;
            restoredEntries.add(restored);
            if (i <= currentIndex) restoredCurrentIndex = restoredEntries.size() - 1;
        }

        int selectedIndex = restoredCurrentIndex;
        Main.runLater(() -> Main.getDisplayManager().getNavigationHistory()
                .restore(restoredEntries, selectedIndex));
        return true;
    }

    private static NavigationEntry restoreEntry(Trinity trinity, DatabaseNavigationEntry entry) {
        ClassInput owner = trinity.getExecution().getClassInput(entry.getClassName());
        if (owner == null) return null;

        InputType inputType;
        NavigationAction action;
        try {
            inputType = InputType.valueOf(entry.getInputType());
            action = NavigationAction.valueOf(entry.getAction());
        } catch (IllegalArgumentException exception) {
            return null;
        }

        Input<?> input = switch (inputType) {
            case CLASS -> owner;
            case METHOD -> owner.getMethod(entry.getMemberName(), entry.getMemberDescriptor());
            case FIELD -> owner.getField(entry.getMemberName(), entry.getMemberDescriptor());
            default -> null;
        };
        if (input == null) return null;

        AbstractInsnNode instruction = null;
        if (entry.getInstructionIndex() >= 0 && input instanceof MethodInput method
                && entry.getInstructionIndex() < method.getInstructions().size()) {
            instruction = method.getInstructions().get(entry.getInstructionIndex());
        }
        NavigationTarget target = NavigationTarget.capture(input, instruction);
        return new NavigationEntry(entry.getId(), target, action,
                entry.getTimestampMillis(), entry.getDisplayText());
    }

    @Override
    protected int databaseHashCode() {
        return Objects.hash("navigationHistory");
    }
}
