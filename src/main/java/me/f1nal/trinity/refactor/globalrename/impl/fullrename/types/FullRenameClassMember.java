package me.f1nal.trinity.refactor.globalrename.impl.fullrename.types;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MemberInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.components.MemorableCheckboxComponent;
import me.f1nal.trinity.refactor.globalrename.api.GlobalRenameContext;

public abstract class FullRenameClassMember<I extends MemberInput<?>> extends FullRenameMember {
    private final Class<I> type;

    protected FullRenameClassMember(String label, String namePrefix, Class<I> type) {
        super(label, namePrefix);
        this.type = type;
    }

    @Override
    public final void refactor(GlobalRenameContext context) {
        for (ClassInput classInput : context.execution().getClassList()) {
            this.prepare();

            // :P
            if (type == MethodInput.class) {
                //noinspection unchecked
                classInput.getMethodMap().values().forEach(method -> refactorMember((I) method, context));
            } else {
                //noinspection unchecked
                classInput.getFieldMap().values().forEach(field -> refactorMember((I) field, context));
            }
        }
    }

    protected abstract void refactorMember(I member, GlobalRenameContext context);
}
