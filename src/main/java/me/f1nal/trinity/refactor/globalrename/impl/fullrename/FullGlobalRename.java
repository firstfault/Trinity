package me.f1nal.trinity.refactor.globalrename.impl.fullrename;

import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameMember;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameClasses;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameFields;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenameMethods;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl.FullRenamePackages;

import javax.annotation.Nullable;
import java.util.List;

public final class FullGlobalRename extends GlobalRenameType {
    private final List<FullRenameMember> renameMembers = List.of(
            new FullRenamePackages(),
            new FullRenameClasses(),
            new FullRenameFields(),
            new FullRenameMethods()
    );

    public FullGlobalRename() {
        super("Full Rename", "Renames all classes/methods/fields. Used to have a more readable jar.");
    }

    @Override
    public void drawInputs() {
        renameMembers.forEach(FullRenameMember::draw);
    }

    @Override
    public void refactor(GlobalRenameContext context) {
        renameMembers.stream().filter(FullRenameMember::isEnabled).forEach(r -> {
            r.prepare();
            r.refactor(context);
        });
    }
}
