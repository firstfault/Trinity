package me.f1nal.trinity.refactor.globalrename.impl;

import imgui.ImGui;
import imgui.type.ImInt;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.remap.NameHeuristics;
import me.f1nal.trinity.remap.Remapper;
import me.f1nal.trinity.util.NameUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public final class FullGlobalRenameType extends GlobalRenameType {
    public FullGlobalRenameType() {
        super("Full Rename", "Renames all classes/methods/fields. Used to have a more readable jar.");
    }

    @Override
    public void drawInputs() {
    }

    @Override
    public void runRefactor(final Execution execution, final List<Rename> renames, NameHeuristics nameHeuristics) {
        int classCount = 0, packageCount = 0;

        for (final var classInput : execution.getClassList()) {
            if (nameHeuristics.isNameObfuscated(classInput.getDisplaySimpleName(), InputType.CLASS)) {
                renames.add(new Rename(classInput, classInput.getClassTarget().getPackage().getChildrenPath(generateName("Class", ++classCount))));
            }

            int methodCount = 0;
            for (final var methodInput : classInput.getMethodMap().values()) {
                if (nameHeuristics.isNameObfuscated(methodInput.getDisplayName().getName(), methodInput.getType())) {
                    renames.add(new Rename(methodInput, generateName("method", ++methodCount)));
                }
            }

            int fieldCount = 0;
            for (final var fieldInput : classInput.getFieldMap().values()) {
                if (nameHeuristics.isNameObfuscated(fieldInput.getDisplayName().getName(), fieldInput.getType())) {
                    renames.add(new Rename(fieldInput, generateName("field", ++fieldCount)));
                }
            }
        }

        // FIXME: Doesn't rename B if A contains B and both are obfuscated.
        List<Package> packages = execution.getAllPackages();
        for (Package pkg : packages) {
            if (pkg.isArchive() || !nameHeuristics.isNameObfuscated(pkg.getName(), InputType.PACKAGE)) {
                continue;
            }

            renames.add(new Rename(pkg::rename, generateName("package", ++packageCount)));
        }
    }

    private String generateName(final @Nullable String prefix, final int count) {
        return prefix + count;
    }

}
