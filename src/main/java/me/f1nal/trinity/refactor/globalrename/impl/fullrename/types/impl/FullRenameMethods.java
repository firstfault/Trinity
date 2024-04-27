package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.impl;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;
import me.f1nal.trinity.refactor.globalrename.api.Rename;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.types.FullRenameClassMember;

public class FullRenameMethods extends FullRenameClassMember<MethodInput> {
    public FullRenameMethods() {
        super("Methods", "method", MethodInput.class);
    }

    @Override
    protected void refactorMember(MethodInput member, GlobalRenameContext context) {
        if (context.nameHeuristics().isObfuscated(member.getDisplayName().getName(), member.getType())) {
            context.renames().add(new Rename(member, generateName()));
        }
    }
}
