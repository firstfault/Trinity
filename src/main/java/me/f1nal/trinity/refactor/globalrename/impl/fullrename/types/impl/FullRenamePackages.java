package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameMember;

import java.util.List;

public class FullRenamePackages extends FullRenameMember {
    public FullRenamePackages() {
        super("Packages", "pkg");
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        // FIXME: Doesn't rename B if A contains B and both are obfuscated.
        List<Package> packages = context.execution().getAllPackages();
        for (Package pkg : packages) {
            if (pkg.isArchive() || !context.nameHeuristics().isObfuscated(pkg.getName(), InputType.PACKAGE)) {
                continue;
            }

            context.renames().add(new Rename(pkg::rename, generateName()));
        }
    }
}
