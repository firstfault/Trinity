package me.f1nal.trinity.refactor;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.refactor.globalrename.GlobalRenameType;
import me.f1nal.trinity.refactor.globalrename.impl.fullrename.FullGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.EnumFieldsGlobalRename;
import me.f1nal.trinity.refactor.globalrename.impl.MixinGlobalRename;

import java.util.List;

public class RefactorManager {
    private final Trinity trinity;
    private final List<GlobalRenameType> globalRenameTypes = List.of(new FullGlobalRename(), new EnumFieldsGlobalRename(), new MixinGlobalRename());

    public RefactorManager(Trinity trinity) {
        this.trinity = trinity;
    }

    public List<GlobalRenameType> getGlobalRenameTypes() {
        return globalRenameTypes;
    }

    public Trinity getTrinity() {
        return trinity;
    }
}
