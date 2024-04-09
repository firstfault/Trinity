package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.util.INameable;

import java.util.List;

public abstract class ConstantSearchType implements INameable {
    private final String name;
    private final Trinity trinity;

    protected ConstantSearchType(String name, Trinity trinity) {
        this.name = name;
        this.trinity = trinity;
    }

    @Override
    public String getName() {
        return name;
    }

    public Trinity getTrinity() {
        return trinity;
    }

    public abstract boolean draw();
    public abstract void populate(List<ConstantViewCache> list);
}
