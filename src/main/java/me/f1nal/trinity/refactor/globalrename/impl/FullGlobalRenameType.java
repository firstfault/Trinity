package me.f1nal.trinity.refactor.globalrename.impl;

import imgui.ImGui;
import imgui.type.ImInt;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.ClassRename;
import me.f1nal.trinity.refactor.globalrename.api.FieldRename;
import me.f1nal.trinity.refactor.globalrename.api.MethodRename;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public final class FullGlobalRenameType extends GlobalRenameType {
    private final ImInt renameChoice;

    public FullGlobalRenameType() {
        super("Full Rename", "Renames all classes/methods/fields. Used to have a more readable jar.");
        this.renameChoice = new ImInt(0);
    }

    @Override
    public void drawInputs() {
        ImGui.combo("Rename type", renameChoice, GlobalRenameNameType.valuesToStringArray());
    }

    @Override
    public void runRefactor(final Execution execution, final List<Rename<?>> renames) {
        final var nameType = GlobalRenameNameType.values()[renameChoice.get()];
        int count = 0;

        for (final var classInput : execution.getClassList()) {
            renames.add(new ClassRename(classInput.getClassTarget(), generateName(nameType, "Class", ++count)));

            int methodCount = 0;
            for (final var methodInput : classInput.getMethodList().values()) {
                renames.add(new MethodRename(methodInput, generateName(nameType, "method", ++methodCount)));
            }

            int fieldCount = 0;
            for (final var fieldInput : classInput.getFieldList().values()) {
                renames.add(new FieldRename(fieldInput, generateName(nameType, "field", ++fieldCount)));
            }
        }
    }

    private String generateName(final @NotNull GlobalRenameNameType nameType, final @Nullable String prefix, final int count) {
        return switch (nameType) {
            case PREFIX_AND_COUNT -> prefix + count;
            case RANDOM_SEQUENCE -> NameUtil.generateRandomSequence(12); // FIXME: there is a VERY SMALL chance that a name gets generated twice
            case RANDOM_WORDS -> generateWord(count);
        };
    }

    private String generateWord(final int count) {
        final var words = NameUtil.getWordList();
        final var len = words.length;
        final var index = count % len;
        final var timesIterated = count / len;

        return words[index] + (timesIterated == 0 ? "" : timesIterated);
    }

    private enum GlobalRenameNameType {
        PREFIX_AND_COUNT("Prefix + count"),
        RANDOM_SEQUENCE("Random sequence"),
        RANDOM_WORDS("Random words");

        private final String displayName;

        GlobalRenameNameType(final @NotNull String displayName) {
            this.displayName = displayName;
        }

        private static String[] valuesToStringArray() {
            return Stream.of(GlobalRenameNameType.values())
                    .map(value -> value.displayName)
                    .toArray(String[]::new);
        }
    }
}
