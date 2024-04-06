package me.f1nal.trinity.gui.frames.impl.project.create.misc;

import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.IDescribable;
import me.f1nal.trinity.util.INameable;

import java.util.function.BooleanSupplier;

public class ClassPathViewerElement implements INameable, IDescribable {
    private final String name;
    private final String size;
    private final BooleanSupplier remove;

    public ClassPathViewerElement(String name, long size, BooleanSupplier remove) {
        this.name = name;
        this.size = ByteUtil.getHumanReadableByteCountSI(size);
        this.remove = remove;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.size;
    }

    public boolean remove() {
        return remove.getAsBoolean();
    }
}
